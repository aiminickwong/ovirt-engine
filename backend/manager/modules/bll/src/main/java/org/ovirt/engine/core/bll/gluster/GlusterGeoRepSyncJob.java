package org.ovirt.engine.core.bll.gluster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.gluster.GeoRepSessionStatus;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterGeoRepSession;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterGeoRepSessionDetails;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeEntity;
import org.ovirt.engine.core.common.constants.gluster.GlusterConstants;
import org.ovirt.engine.core.common.gluster.GlusterFeatureSupported;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.common.vdscommands.VDSReturnValue;
import org.ovirt.engine.core.common.vdscommands.gluster.GlusterVolumeGeoRepSessionVDSParameters;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.utils.timer.OnTimerMethodAnnotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GlusterGeoRepSyncJob extends GlusterJob {
    private static final Logger log = LoggerFactory.getLogger(GlusterGeoRepSyncJob.class);

    private static final GlusterGeoRepSyncJob instance = new GlusterGeoRepSyncJob();
    private static final GeoRepSessionStatus[] overridableStatuses = { GeoRepSessionStatus.ACTIVE,
            GeoRepSessionStatus.INITIALIZING,
            GeoRepSessionStatus.NOTSTARTED
    };

    public void init() {
        log.info("Gluster georeplication monitoring has been initialized");
    }

    public static GlusterGeoRepSyncJob getInstance() {
        return instance;
    }

    @OnTimerMethodAnnotation("gluster_georep_poll_event")
    public void refreshGeoRepData() {
        // get all clusters
        List<VDSGroup> clusters = getClusterDao().getAll();
        // for every cluster that supports geo-rep monitoring
        for (VDSGroup cluster : clusters) {
            refreshGeoRepDataInCluster(cluster);
        }

    }

    public void refreshGeoRepDataInCluster(VDSGroup cluster) {
        if (!supportsGlusterGeoRepFeature(cluster)) {
            return;
        }

        Map<String, GlusterGeoRepSession> sessionsMap = getSessionsForCluster(cluster);
        if (sessionsMap == null) {
            log.debug("Error in retrieving sessions for cluster '{}' from CLI, nothing to do", cluster.getName());
            return;
        }

        removeDeletedSessions(cluster.getId(), sessionsMap);

        // for each geo-rep session, find session in database and update details.
        for (GlusterGeoRepSession session : sessionsMap.values()) {
            GlusterVolumeEntity masterVolume = getVolume(cluster, session.getMasterVolumeName());
            if (masterVolume == null) {
                log.info("Could not find corresponding volume for geo-rep session '{}' and volume '{}' - status will not be updated.",
                        session.getSessionKey(),
                        session.getMasterVolumeName());
            } else {
                session.setMasterVolumeId(masterVolume.getId());
                // update consolidated status
                updateGeoRepStatus(masterVolume, session);
            }

            // check if session exists in database
            GlusterGeoRepSession sessionInDb = getGeoRepDao().getGeoRepSession(session.getSessionKey());
            if (sessionInDb == null) {
                // save the session in database first.
                log.debug("detected new geo-rep session '{}' for volume '{}'",
                        session.getSessionKey(),
                        session.getMasterVolumeName());
                if (Guid.isNullOrEmpty(session.getId())) {
                    session.setId(Guid.newGuid());
                }
                getGeoRepDao().save(session);
                logGeoRepMessage(AuditLogType.GLUSTER_GEOREP_SESSION_DETECTED_FROM_CLI, cluster.getId(), session);
            } else {
                session.setId(sessionInDb.getId());
                getGeoRepDao().updateSession(session);
            }
            //update the session details object with session id.
            for (GlusterGeoRepSessionDetails sessDetails : session.getSessionDetails()) {
                sessDetails.setSessionId(session.getId());
            }
            getGeoRepDao().saveOrUpdateDetailsInBatch(session.getSessionDetails());
        }

    }

    private void removeDeletedSessions(Guid clusterId, final Map<String, GlusterGeoRepSession> sessionsMap) {
        List<GlusterGeoRepSession> sessionsInDb = getGeoRepDao().getGeoRepSessionsInCluster(clusterId);
        if (sessionsInDb == null || sessionsInDb.isEmpty()) {
            return;
        }
        List<GlusterGeoRepSession> sessionsToDelete = new ArrayList<>();
        for (GlusterGeoRepSession grepSession: sessionsInDb) {
            if (sessionsMap.get(grepSession.getSessionKey()) == null) {
                sessionsToDelete.add(grepSession);
            }
        }

        for (final GlusterGeoRepSession session : sessionsToDelete) {
            log.debug("geo-rep session '{}' detected removed for volume '{}'",
                    session.getSessionKey(),
                    session.getMasterVolumeName());
            getGeoRepDao().remove(session.getId());
            logGeoRepMessage(AuditLogType.GLUSTER_GEOREP_SESSION_DELETED_FROM_CLI, clusterId, session);
        }
    }

    private void logGeoRepMessage(AuditLogType logType, Guid clusterId, final GlusterGeoRepSession session) {
        logUtil.logAuditMessage(clusterId, null, null,
                logType,
                new HashMap<String, String>() {
                    {
                        put(GlusterConstants.VOLUME_NAME, session.getMasterVolumeName());
                        put("geoRepSessionKey", session.getSessionKey());
                    }
                });
    }

    /**
     * This method updates the status depending on health of individual nodes
     *
     * @param volume
     * @param session
     */
    private void updateGeoRepStatus(GlusterVolumeEntity volume, GlusterGeoRepSession session) {

        List<HashSet<GeoRepSessionStatus>> list = new ArrayList<>();
        // grouped node status
        int replicaCount = volume.getReplicaCount() == 0 ? 1 : volume.getReplicaCount();
        for (int i = 0; i < volume.getBricks().size(); i = i + replicaCount) {
            HashSet<GeoRepSessionStatus> subVolumeStatusSet = new HashSet<>();
            int j = 0;
            while (j < replicaCount) {
                Guid brickId = volume.getBricks().get(i + j).getId();
                subVolumeStatusSet.add(getStatusForBrickFromSession(session, brickId));
                j++;
            }
            list.add(subVolumeStatusSet);
        }

        session.setStatus(GeoRepSessionStatus.ACTIVE);
        // iterate through grouped status to set consolidated status
        for (HashSet<GeoRepSessionStatus> subVolumeStatusValues : list) {
            if (subVolumeStatusValues.contains(GeoRepSessionStatus.ACTIVE)) {
                // healthy
                continue;
            } else if (subVolumeStatusValues.contains(GeoRepSessionStatus.FAULTY)) {
                session.setStatus(GeoRepSessionStatus.FAULTY);
                // if any one of the sub-volume is faulty, the overall session status if faulty
                return;
            }
            // override status in case of these values
            if (ArrayUtils.contains(overridableStatuses, session.getStatus())) {
                if (subVolumeStatusValues.size() == 1) {
                    session.setStatus((GeoRepSessionStatus) subVolumeStatusValues.toArray()[0]);
                } else {
                    // if status values in sub-volume are not the same, what do we do?
                    // this should not happen, so we'll log it for now
                    log.info("Multiple status values found in volume '{}'", session.getMasterVolumeName());
                }
            }
        }

    }

    private GeoRepSessionStatus getStatusForBrickFromSession(GlusterGeoRepSession session, Guid masterBrickId) {
        if (session.getSessionDetails() == null) {
            return null;
        }
        for (GlusterGeoRepSessionDetails sessionDetail : session.getSessionDetails()) {
            if (sessionDetail.getMasterBrickId().equals(masterBrickId)) {
                return sessionDetail.getStatus();
            }
        }
        return GeoRepSessionStatus.UNKNOWN;
    }

    private Map<String, GlusterGeoRepSession> getSessionsForCluster(VDSGroup cluster) {
        VDS upServer = getClusterUtils().getRandomUpServer(cluster.getId());
        if (upServer == null) {
            log.debug("No UP server found in cluster '{}' for geo-rep monitoring", cluster.getName());
            return null;
        }
        // get details of geo-rep sessions in cluster
        VDSReturnValue returnValue = runVdsCommand(VDSCommandType.GetGlusterVolumeGeoRepStatus,
                new GlusterVolumeGeoRepSessionVDSParameters(upServer.getId(), null));
        if (returnValue.getSucceeded()) {
            List<GlusterGeoRepSession> sessions = (List<GlusterGeoRepSession>) returnValue.getReturnValue();
            HashMap<String, GlusterGeoRepSession> sessionsMap = new HashMap<>();
            if (sessions == null) {
                return sessionsMap;
            }
            for (GlusterGeoRepSession session : sessions) {
                sessionsMap.put(session.getSessionKey(), session);
            }
            return sessionsMap;
        } else {
            log.error("VDS error {}", returnValue.getVdsError().getMessage());
            log.debug("VDS error", returnValue.getVdsError());
            return null;
        }

    }

    private GlusterVolumeEntity getVolume(VDSGroup cluster, String masterVolumeName) {
        return getVolumeDao().getByName(cluster.getId(), masterVolumeName);
    }

    private boolean supportsGlusterGeoRepFeature(VDSGroup cluster) {
        return cluster.supportsGlusterService()
                && GlusterFeatureSupported.glusterGeoReplication(cluster.getcompatibility_version());
    }

}
