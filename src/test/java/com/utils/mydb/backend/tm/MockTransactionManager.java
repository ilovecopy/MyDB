package com.utils.mydb.backend.tm;

import com.utils.mydb.backend.dm.tm.TransactionManager;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MockTransactionManager implements TransactionManager {

    private static final byte FIELD_TRAN_ACTIVE = 0;
    private static final byte FIELD_TRAN_COMMITTED = 1;
    private static final byte FIELD_TRAN_ABORTED = 2;

    public static final long SUPER_XID = 0;

    private Map<Long, Byte> transactions;
    private long xidCounter;
    private Lock counterLock;

    public static MockTransactionManager newMockTransactionManager() {
        MockTransactionManager tm = new MockTransactionManager();
        tm.transactions = new HashMap<>();
        tm.xidCounter = 0;
        tm.counterLock = new ReentrantLock();
        return tm;
    }

    @Override
    public long begin() {
        counterLock.lock();
        try {
            long xid = xidCounter + 1;
            transactions.put(xid, FIELD_TRAN_ACTIVE);
            xidCounter = xid;
            return xid;
        } finally {
            counterLock.unlock();
        }
    }

    @Override
    public void commit(long xid) {
        transactions.put(xid, FIELD_TRAN_COMMITTED);
    }

    @Override
    public void abort(long xid) {
        transactions.put(xid, FIELD_TRAN_ABORTED);
    }

    @Override
    public boolean isActive(long xid) {
        if (xid == SUPER_XID) return false;
        Byte status = transactions.get(xid);
        return status != null && status == FIELD_TRAN_ACTIVE;
    }

    @Override
    public boolean isCommitted(long xid) {
        if (xid == SUPER_XID) return true;
        Byte status = transactions.get(xid);
        return status != null && status == FIELD_TRAN_COMMITTED;
    }

    @Override
    public boolean isAborted(long xid) {
        if (xid == SUPER_XID) return false;
        Byte status = transactions.get(xid);
        return status != null && status == FIELD_TRAN_ABORTED;
    }

    @Override
    public void close() {
    }

}
