package com.utils.mydb.backend.vm;

import com.utils.mydb.backend.common.AbstractCache;
import com.utils.mydb.backend.dm.DataManager;
import com.utils.mydb.backend.dm.tm.TransactionManager;
import com.utils.mydb.backend.dm.tm.TransactionManagerImpl;
import com.utils.mydb.backend.utils.Panic;
import com.utils.mydb.common.Error;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class VersionManagerImpl extends AbstractCache<Entry> implements VersionManager {

    TransactionManager tm;
    DataManager dm;
    Map<Long, Transaction> activeTransaction;
    Lock lock;
    LockTable lt;

    protected VersionManagerImpl(TransactionManager tm, DataManager dm) {
        super(0);
        this.tm = tm;
        this.dm = dm;
        this.activeTransaction = new HashMap<>();
        activeTransaction.put(TransactionManagerImpl.SUPER_XID, Transaction.newTransaction(TransactionManagerImpl.SUPER_XID, 0, null));
        this.lock = new ReentrantLock();
        this.lt = new LockTable();
    }

    public void releaseEntry(Entry entry) {
        super.release(entry.getUid());
    }

    @Override
    protected Entry getForCache(long uid) throws Exception {
        Entry entry = Entry.loadEntry(this, uid);
        if (entry == null) {
            Panic.panic(Error.NULL_ENTRY_EXCEPTION);
        }
        return entry;
    }

    @Override
    protected void releaseForCache(Entry entry) {
        entry.remove();
    }

    @Override
    public byte[] read(long xid, long uid) throws Exception {
        lock.lock();
        Transaction t = activeTransaction.get(xid);
        lock.unlock();
        if (t.err != null) {
            throw t.err;
        }
        Entry entry;
        try {
            entry = super.get(uid);
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().equals(Error.NULL_ENTRY_EXCEPTION.getMessage())) {
                return null;
            } else {
                throw e;
            }
        }
        try {
            if (Visibility.isVisible(tm, t, entry)) {
                return entry.data();
            } else {
                return null;
            }
        } finally {
            entry.release();
        }
    }

    @Override
    public long insert(long xid, byte[] data) throws Exception {
        lock.lock();
        Transaction t = activeTransaction.get(xid);
        lock.unlock();
        if (t.err != null) {
            throw t.err;
        }
        byte[] raw = Entry.wrapEntryRaw(xid, data);
        return dm.insert(xid, raw);
    }

    @Override
    public boolean delete(long xid, long uid) throws Exception {
        lock.lock();
        Transaction t = activeTransaction.get(xid);
        lock.unlock();
        if (t.err != null) {
            throw t.err;
        }
        Entry entry;
        try {
            entry = super.get(uid);
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().equals(Error.NULL_ENTRY_EXCEPTION.getMessage())) {
                return false;
            } else {
                throw e;
            }
        }
        try {
            if (!Visibility.isVisible(tm, t, entry)) {
                return false;
            }
            Lock l;
            try {
                l = lt.add(uid, xid);
            } catch (Exception e) {
                t.err = new RuntimeException(Error.CONCURRENT_UPDATE_EXCEPTION.getMessage());
                internAbort(xid, true);
                t.autoAborted = true;
                throw t.err;
            }
            if (l != null) {
                l.lock();
                l.unlock();
            }
            if (entry.getXmax() == xid) {
                return false;
            }
            if (Visibility.isVersionSkip(tm, t, entry)) {
                t.err = new RuntimeException(Error.CONCURRENT_UPDATE_EXCEPTION.getMessage());
                internAbort(xid, true);
                t.autoAborted = true;
                throw t.err;
            }
            entry.setXmax(xid);
            return true;
        } finally {
            entry.release();
        }
    }

    /**
     * level 0:RC
     * level 1:RR
     *
     * @param level
     * @return long
     */
    @Override
    public long begin(int level) {
        lock.lock();
        try {
            long xid = tm.begin();
            Transaction t = Transaction.newTransaction(xid, level, activeTransaction);
            activeTransaction.put(xid, t);
            return xid;
        } catch (Exception e) {
            Panic.panic(e);
        } finally {
            lock.unlock();
        }
        return 0;
    }

    @Override
    public void commit(long xid) throws Exception {
        lock.lock();
        Transaction t = activeTransaction.get(xid);
        lock.unlock();
        try {
            if (t.err != null) {
                throw t.err;
            }
        } catch (NullPointerException n) {
            System.out.println(xid);
            System.out.println(activeTransaction.keySet());
            Panic.panic(n);
        }
        lock.lock();
        activeTransaction.remove(xid);
        lock.unlock();
        lt.remove(xid);
        tm.commit(xid);
    }

    @Override
    public void abort(long xid) {
        internAbort(xid, false);
    }

    private void internAbort(long xid, boolean autoAborted) {
        lock.lock();
        Transaction t = activeTransaction.get(xid);
        if (!autoAborted) {
            activeTransaction.remove(xid);
        }
        lock.unlock();

        if (t.autoAborted) {
            return;
        }
        lt.remove(xid);
        tm.abort(xid);
    }
}
