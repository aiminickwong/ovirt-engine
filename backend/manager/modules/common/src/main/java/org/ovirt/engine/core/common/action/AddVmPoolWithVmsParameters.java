package org.ovirt.engine.core.common.action;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.validation.Valid;

import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.common.businessentities.GraphicsDevice;
import org.ovirt.engine.core.common.businessentities.GraphicsType;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VmRngDevice;
import org.ovirt.engine.core.common.businessentities.VmStatic;
import org.ovirt.engine.core.common.businessentities.VmPool;
import org.ovirt.engine.core.compat.Guid;

public class AddVmPoolWithVmsParameters extends VmPoolOperationParameters {
    private static final long serialVersionUID = 4826143612049185740L;

    @Valid
    private VM _vm;
    private int _vmsCount;
    private int _diskSize;
    private HashMap<Guid, DiskImage> diskInfoDestinationMap;
    private Boolean soundDeviceEnabled;
    private Boolean consoleEnabled;
    private Boolean virtioScsiEnabled;
    private boolean balloonEnabled;
    private VmRngDevice rngDevice;
    private Map<GraphicsType, GraphicsDevice> graphicsDevices;

    public AddVmPoolWithVmsParameters() {
        init();
    }

    public AddVmPoolWithVmsParameters(VmPool vmPool, VM vm, int count, int diskSize) {
        super(vmPool);
        init();
        _vm = vm;
        _vmsCount = count;
        _diskSize = diskSize;
    }

    private void init() {
        graphicsDevices = new HashMap<GraphicsType, GraphicsDevice>();
    }

    @Valid
    public VmStatic getVmStaticData() {
        return _vm.getStaticData();
    }

    public int getVmsCount() {
        return _vmsCount;
    }

    public int getDiskSize() {
        return _diskSize;
    }

    public HashMap<Guid, DiskImage> getDiskInfoDestinationMap() {
        return diskInfoDestinationMap;
    }

    public void setDiskInfoDestinationMap(HashMap<Guid, DiskImage> diskInfoDestinationMap) {
        this.diskInfoDestinationMap = diskInfoDestinationMap;
    }

    public Boolean isSoundDeviceEnabled() {
        return soundDeviceEnabled;
    }

    public void setSoundDeviceEnabled(boolean soundDeviceEnabled) {
        this.soundDeviceEnabled = soundDeviceEnabled;
    }


    public Boolean isConsoleEnabled() {
        return consoleEnabled;
    }

    public void setConsoleEnabled(Boolean consoleEnabled) {
        this.consoleEnabled = consoleEnabled;
    }

    public Boolean isVirtioScsiEnabled() {
        return virtioScsiEnabled;
    }

    public void setVirtioScsiEnabled(Boolean virtioScsiEnabled) {
        this.virtioScsiEnabled = virtioScsiEnabled;
    }

    public VmRngDevice getRngDevice() {
        return rngDevice;
    }

    public void setRngDevice(VmRngDevice rngDevice) {
        this.rngDevice = rngDevice;
    }

    public boolean isBalloonEnabled() {
        return balloonEnabled;
    }

    public void setBalloonEnabled(boolean isBallonEnabled) {
        this.balloonEnabled = isBallonEnabled;
    }

    public Collection<GraphicsDevice> getGraphicsDevices() {
        return graphicsDevices.values();
    }

    public void addGraphicsDevice(GraphicsDevice graphicsDevice) {
        graphicsDevices.put(graphicsDevice.getGraphicsType(), graphicsDevice);
    }
}
