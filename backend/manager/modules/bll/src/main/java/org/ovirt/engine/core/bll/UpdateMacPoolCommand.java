package org.ovirt.engine.core.bll;

import java.util.Collections;
import java.util.List;

import org.ovirt.engine.core.bll.network.macpoolmanager.MacPoolPerDcSingleton;
import org.ovirt.engine.core.bll.utils.PermissionSubject;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.MacPoolParameters;
import org.ovirt.engine.core.common.businessentities.ActionGroup;
import org.ovirt.engine.core.common.businessentities.MacPool;
import org.ovirt.engine.core.common.errors.VdcBllMessages;
import org.ovirt.engine.core.common.validation.group.UpdateEntity;
import org.ovirt.engine.core.compat.Guid;

public class UpdateMacPoolCommand extends MacPoolCommandBase<MacPoolParameters> {

    private MacPool oldMacPool;

    public UpdateMacPoolCommand(MacPoolParameters parameters) {
        super(parameters);
        addValidationGroup(UpdateEntity.class);
    }

    @Override
    protected void setActionMessageParameters() {
        super.setActionMessageParameters();
        addCanDoActionMessage(VdcBllMessages.VAR__ACTION__UPDATE);
    }

    @Override
    protected boolean canDoAction() {
        if (!super.canDoAction()) {
            return false;
        }

        oldMacPool = getMacPoolDao().get(getMacPoolId());
        return validate(new MacPoolValidator(oldMacPool).macPoolExists()) &&
                validate(new MacPoolValidator(getMacPoolEntity()).hasUniqueName()) &&
                validate(validateDefaultFlagIsNotChanged(oldMacPool, getMacPoolEntity()));
    }

    private MacPool getMacPoolEntity() {
        return getParameters().getMacPool();
    }

    private Guid getMacPoolId() {
        return getMacPoolEntity().getId();
    }

    @Override
    protected void executeCommand() {
        getMacPoolDao().update(getMacPoolEntity());
        MacPoolPerDcSingleton.getInstance().modifyPool(getMacPoolEntity());

        setSucceeded(true);
        getReturnValue().setActionReturnValue(getMacPoolId());
    }

    @Override
    public List<PermissionSubject> getPermissionCheckSubjects() {
        return Collections.singletonList(new PermissionSubject(MultiLevelAdministrationHandler.SYSTEM_OBJECT_ID,
                VdcObjectType.System, ActionGroup.CONFIGURE_ENGINE));
    }

    @Override
    public void rollback() {
        super.rollback();
        MacPoolPerDcSingleton.getInstance().modifyPool(oldMacPool);
    }

    static ValidationResult validateDefaultFlagIsNotChanged(MacPool macPoolFromDb, MacPool newMacPool) {
        if (macPoolFromDb == null || newMacPool == null) {
            throw new IllegalArgumentException();
        }

        final boolean defaultChanged = macPoolFromDb.isDefaultPool() != newMacPool.isDefaultPool();
        return ValidationResult.failWith(
                VdcBllMessages.ACTION_TYPE_FAILED_CHANGING_DEFAULT_MAC_POOL_IS_NOT_SUPPORTED)
                .when(defaultChanged);
    }
}