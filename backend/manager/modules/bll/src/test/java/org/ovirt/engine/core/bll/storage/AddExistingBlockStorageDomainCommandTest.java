package org.ovirt.engine.core.bll.storage;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.ovirt.engine.core.common.action.StorageDomainManagementParameter;
import org.ovirt.engine.core.common.businessentities.LUNs;
import org.ovirt.engine.core.common.businessentities.StorageDomainStatic;
import org.ovirt.engine.core.common.errors.VdcBllMessages;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.dao.StorageDomainStaticDAO;

@RunWith(MockitoJUnitRunner.class)
public class AddExistingBlockStorageDomainCommandTest {

    private AddExistingBlockStorageDomainCommand<StorageDomainManagementParameter> command;
    private StorageDomainManagementParameter parameters;

    @Mock
    private DbFacade dbFacade;

    @Mock
    private StorageDomainStaticDAO storageDomainStaticDAO;

    @Before
    public void setUp() {
        parameters = new StorageDomainManagementParameter(getStorageDomain());
        parameters.setVdsId(Guid.newGuid());
        command = spy(new AddExistingBlockStorageDomainCommand<>(parameters));
        doReturn(dbFacade).when(command).getDbFacade();
        doReturn(storageDomainStaticDAO).when(command).getStorageDomainStaticDAO();

        doNothing().when(command).addStorageDomainInDb();
        doNothing().when(command).updateStorageDomainDynamicFromIrs();
        doNothing().when(command).saveLUNsInDB(anyList());
    }

    @Test
    public void testAddExistingBlockDomainSuccessfully() {
        when(command.getStorageDomainStaticDAO().get(any(Guid.class))).thenReturn(null);
        when(command.getLUNsFromVgInfo(parameters.getStorageDomain().getStorage())).thenReturn(getLUNs());
        command.executeCommand();
        assertTrue(command.getReturnValue().getSucceeded());
    }

    @Test
    public void testAlreadyExistStorageDomain() {
        when(command.getStorageDomainStaticDAO().get(any(Guid.class))).thenReturn(getStorageDomain());
        assertFalse("Storage Domain already exists", command.canAddDomain());
        assertTrue("Import block Storage Domain should have failed due to already existing Storage Domain",
                command.getReturnValue()
                        .getCanDoActionMessages()
                        .contains(VdcBllMessages.ACTION_TYPE_FAILED_STORAGE_DOMAIN_ALREADY_EXIST.toString()));
    }

    private static StorageDomainStatic getStorageDomain() {
        StorageDomainStatic storageDomain = new StorageDomainStatic();
        storageDomain.setStorage(Guid.newGuid().toString());
        return storageDomain;
    }

    private static List<LUNs> getLUNs() {
        List<LUNs> luns = new ArrayList<>();
        LUNs lun = new LUNs();
        lun.setId(Guid.newGuid().toString());
        luns.add(lun);
        return luns;
    }
}
