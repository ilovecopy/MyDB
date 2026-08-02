package com.utils.mydb.backend.vm;

import com.utils.mydb.common.Error;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.utils.mydb.backend.dm.MockDataManager;
import com.utils.mydb.backend.dm.tm.TransactionManagerImpl;
import com.utils.mydb.backend.tm.MockTransactionManager;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;


public class VMTest {

    private VersionManager newVM() {
        MockTransactionManager tm = MockTransactionManager.newMockTransactionManager();
        MockDataManager dm = MockDataManager.newMockDataManager();
        return VersionManager.newVersionManager(tm, dm);
    }

    @Test
    public void testInsertAndReadBySuper() throws Exception {
        VersionManager vm = newVM();
        long xid = TransactionManagerImpl.SUPER_XID;
        byte[] data = "Hello VM".getBytes();
        long uid = vm.insert(xid, data);
        byte[] result = vm.read(xid, uid);
        assertNotNull(result);
        assertTrue(Arrays.equals(data, result));
    }

    @Test
    public void testInsertAndReadInSameTx() throws Exception {
        VersionManager vm = newVM();
        long xid = vm.begin(0);
        byte[] data = "Hello TX".getBytes();
        long uid = vm.insert(xid, data);
        byte[] result = vm.read(xid, uid);
        assertNotNull(result);
        assertTrue(Arrays.equals(data, result));
        vm.commit(xid);
    }

    @Test
    public void testReadCommitted_NormalCommit() throws Exception {
        VersionManager vm = newVM();

        long xid1 = vm.begin(0);
        byte[] data = "Data from xid1".getBytes();
        long uid = vm.insert(xid1, data);

        long xid2 = vm.begin(0);
        assertNull(vm.read(xid2, uid));

        vm.commit(xid1);

        long xid3 = vm.begin(0);
        byte[] result = vm.read(xid3, uid);
        assertNotNull(result);
        assertTrue(Arrays.equals(data, result));

        vm.commit(xid2);
        vm.commit(xid3);
    }

    @Test
    public void testReadCommitted_AbortedTx() throws Exception {
        VersionManager vm = newVM();

        long xid1 = vm.begin(0);
        byte[] data = "Data should not be seen".getBytes();
        long uid = vm.insert(xid1, data);

        vm.abort(xid1);

        long xid2 = vm.begin(0);
        assertNull(vm.read(xid2, uid));
        vm.commit(xid2);
    }

    @Test
    public void testReadCommitted_NonRepeatableRead() throws Exception {
        VersionManager vm = newVM();

        long xid1 = vm.begin(0);
        byte[] data1 = "Version1".getBytes();
        long uid = vm.insert(xid1, data1);
        vm.commit(xid1);

        long xidR = vm.begin(0);
        byte[] r1 = vm.read(xidR, uid);
        assertNotNull(r1);
        assertTrue(Arrays.equals(data1, r1));

        long xid2 = vm.begin(0);
        vm.delete(xid2, uid);
        vm.commit(xid2);

        byte[] r2 = vm.read(xidR, uid);
        assertNull(r2);
        vm.commit(xidR);
    }

    @Test
    public void testRepeatableRead_Snapshot() throws Exception {
        VersionManager vm = newVM();

        long xid1 = vm.begin(1);
        byte[] data1 = "Version1".getBytes();
        long uid = vm.insert(xid1, data1);
        vm.commit(xid1);

        long xidR = vm.begin(1);
        byte[] r1 = vm.read(xidR, uid);
        assertNotNull(r1);
        assertTrue(Arrays.equals(data1, r1));

        long xid2 = vm.begin(1);
        byte[] data2 = "Version2".getBytes();
        long uid2 = vm.insert(xid2, data2);
        vm.commit(xid2);

        assertNull(vm.read(xidR, uid2));

        long xid3 = vm.begin(1);
        vm.delete(xid3, uid);
        vm.commit(xid3);

        byte[] r2 = vm.read(xidR, uid);
        assertNotNull(r2);
        assertTrue(Arrays.equals(data1, r2));

        vm.commit(xidR);
    }

    @Test
    public void testDeleteInSameTx() throws Exception {
        VersionManager vm = newVM();
        long xid = vm.begin(0);
        byte[] data = "Delete Me".getBytes();
        long uid = vm.insert(xid, data);
        assertTrue(vm.delete(xid, uid));
        assertNull(vm.read(xid, uid));
        vm.commit(xid);
    }

    @Test
    public void testDeleteCommitted() throws Exception {
        VersionManager vm = newVM();

        long xid1 = vm.begin(0);
        byte[] data = "ToBeDeleted".getBytes();
        long uid = vm.insert(xid1, data);
        vm.commit(xid1);

        long xid2 = vm.begin(0);
        assertTrue(vm.delete(xid2, uid));
        assertNull(vm.read(xid2, uid));
        vm.commit(xid2);

        long xid3 = vm.begin(0);
        assertNull(vm.read(xid3, uid));
        vm.commit(xid3);
    }

    @Test
    public void testDeleteRollback() throws Exception {
        VersionManager vm = newVM();

        long xid1 = vm.begin(0);
        byte[] data = "Survive".getBytes();
        long uid = vm.insert(xid1, data);
        vm.commit(xid1);

        long xid2 = vm.begin(0);
        assertTrue(vm.delete(xid2, uid));
        vm.abort(xid2);

        long xid3 = vm.begin(0);
        byte[] result = vm.read(xid3, uid);
        assertNotNull(result);
        assertTrue(Arrays.equals(data, result));
        vm.commit(xid3);
    }

    @Test
    public void testConcurrentUpdate_VersionSkip() throws Exception {
        VersionManager vm = newVM();

        long xid1 = vm.begin(1);
        byte[] data = "Original".getBytes();
        long uid = vm.insert(xid1, data);
        vm.commit(xid1);

        long xidA = vm.begin(1);
        long xidB = vm.begin(1);

        vm.read(xidA, uid);
        vm.read(xidB, uid);

        assertTrue(vm.delete(xidA, uid));
        vm.commit(xidA);

        AtomicReference<Exception> err = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        new Thread(() -> {
            try {
                vm.delete(xidB, uid);
            } catch (Exception e) {
                err.set(e);
            } finally {
                latch.countDown();
            }
        }).start();
        latch.await();

        assertNotNull(err.get());
        assertTrue(err.get().getMessage().contains(Error.CONCURRENT_UPDATE_EXCEPTION.getMessage()));
    }

    @Test
    public void testDeadlock() throws Exception {
        VersionManager vm = newVM();

        long xidPrep = vm.begin(0);
        byte[] data1 = "Resource1".getBytes();
        long uid1 = vm.insert(xidPrep, data1);
        byte[] data2 = "Resource2".getBytes();
        long uid2 = vm.insert(xidPrep, data2);
        vm.commit(xidPrep);

        long txA = vm.begin(0);
        long txB = vm.begin(0);

        vm.delete(txA, uid1);
        vm.delete(txB, uid2);

        vm.delete(txB, uid1);

        try {
            vm.delete(txA, uid2);
            fail("Should throw DeadlockException");
        } catch (Exception e) {
            boolean isDeadlock = e.getMessage() != null && e.getMessage().contains(Error.DEAD_LOCK_EXCEPTION.getMessage());
            boolean isConcurrent = e.getMessage() != null && e.getMessage().contains(Error.CONCURRENT_UPDATE_EXCEPTION.getMessage());
            assertTrue(isDeadlock || isConcurrent, "Expected deadlock or concurrent update, got: " + e.getMessage());
        }
    }

    @Test
    public void testNullEntry() throws Exception {
        VersionManager vm = newVM();
        long xid = vm.begin(0);
        assertNull(vm.read(xid, 999999L));
        assertFalse(vm.delete(xid, 999999L));
        vm.commit(xid);
    }

    @Test
    public void testAutoAbortedAfterError() throws Exception {
        VersionManager vm = newVM();

        long xid1 = vm.begin(1);
        byte[] data = "Test".getBytes();
        long uid = vm.insert(xid1, data);
        vm.commit(xid1);

        long xidA = vm.begin(1);
        long xidB = vm.begin(1);

        vm.read(xidA, uid);
        vm.read(xidB, uid);

        assertTrue(vm.delete(xidA, uid));
        vm.commit(xidA);

        try {
            vm.delete(xidB, uid);
            fail("Should throw ConcurrentUpdateException");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains(Error.CONCURRENT_UPDATE_EXCEPTION.getMessage()));
        }

        try {
            vm.insert(xidB, "new".getBytes());
            fail("Should still throw error after auto-abort");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains(Error.CONCURRENT_UPDATE_EXCEPTION.getMessage()));
        }
    }

    @Test
    public void testMultiTxInsertRead() throws Exception {
        VersionManager vm = newVM();
        AtomicLong uid1 = new AtomicLong();
        AtomicLong uid2 = new AtomicLong();

        long xid1 = vm.begin(0);
        uid1.set(vm.insert(xid1, "Data1".getBytes()));
        vm.commit(xid1);

        long xid2 = vm.begin(0);
        uid2.set(vm.insert(xid2, "Data2".getBytes()));

        long xidR1 = vm.begin(0);
        assertNotNull(vm.read(xidR1, uid1.get()));
        assertNull(vm.read(xidR1, uid2.get()));
        vm.commit(xidR1);

        vm.commit(xid2);

        long xidR2 = vm.begin(0);
        assertNotNull(vm.read(xidR2, uid1.get()));
        assertNotNull(vm.read(xidR2, uid2.get()));
        vm.commit(xidR2);
    }

}
