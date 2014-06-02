package org.ovirt.engine.core.common.businessentities;

import java.io.Serializable;
import java.util.Map;

public class ExternalHostGroup implements Serializable {
    private static final long serialVersionUID = -3099054972843803212L;

    private String name;

    private int subnetId;
    private int domainId;
    private int environmentId;
    private int hostgroupId;
    private int osId;
    private int ptableId;
    private int mediumId;
    private int architectureId;
    private Map<String, String> parameters;

    public int getHostgroupId() {
        return hostgroupId;
    }
    public void setHostgroupId(int id) {
        this.hostgroupId = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public int getOsId() {
        return osId;
    }
    public void setOsId(int os_id) {
        this.osId = os_id;
    }
    public int getSubnetId() {
        return subnetId;
    }
    public void setSubnetId(int subnetId) {
        this.subnetId = subnetId;
    }
    public int getDomainId() {
        return domainId;
    }
    public void setDomainId(int domainId) {
        this.domainId = domainId;
    }
    public int getEnvironmentId() {
        return environmentId;
    }
    public void setEnvironmentId(int environmentId) {
        this.environmentId = environmentId;
    }
    public Map<String, String> getParameters() {
        return parameters;
    }
    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }
    public int getPtableId() {
        return ptableId;
    }
    public void setPtableId(int ptableId) {
        this.ptableId = ptableId;
    }
    public int getMediumId() {
        return mediumId;
    }
    public void setMediumId(int mediumId) {
        this.mediumId = mediumId;
    }
    public int getArchitectureId() {
        return architectureId;
    }
    public void setArchitectureId(int architectureId) {
        this.architectureId = architectureId;
    }
}