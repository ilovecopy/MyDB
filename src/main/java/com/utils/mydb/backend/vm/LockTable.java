package com.utils.mydb.backend.vm;

import com.utils.mydb.common.Error;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class LockTable {
    private Map<Long, List<Long>> x2u;
    private Map<Long, Long> u2x;
    /**
     * 每张餐桌的等位队列
     */
    private Map<Long, List<Long>> wait;
    private Map<Long, Lock> waitLock;
    /**
     * 顾客正在等哪张餐桌
     */
    private Map<Long, Long> waitU;
    private Lock lock;

    public LockTable() {
        x2u = new HashMap<>();
        u2x = new HashMap<>();
        wait = new HashMap<>();
        waitLock = new HashMap<>();
        waitU = new HashMap<>();
        lock = new ReentrantLock();
    }

    public Lock add(long xid, long uid) throws Exception {
        lock.lock();
        try {
            if (isInList(x2u, xid, uid)) {
                return null;
            }
            if (!u2x.containsKey(uid)) {
                u2x.put(uid, xid);
                putIntoList(x2u, xid, uid);
                return null;
            }
            waitU.put(xid, uid);
            putIntoList(wait, uid, xid);
            if (hasDeadLock()) {
                waitU.remove(xid);
                removeFromList(wait, uid, xid);
                throw new RuntimeException(Error.DEAD_LOCK_EXCEPTION.getMessage());
            }
            Lock l = new ReentrantLock();
            l.lock();
            waitLock.put(xid, l);
            return l;
        } finally {
            lock.unlock();
        }
    }

    public void remove(long xid) {
        lock.lock();
        try {
            List<Long> l = x2u.get(xid);
            if (l != null) {
                while (!l.isEmpty()) {
                    Long uid = l.remove(0);
                    selectNewXID(uid);
                }
            }
            waitU.remove(xid);
            x2u.remove(xid);
            waitLock.remove(xid);
        } finally {
            lock.unlock();
        }
    }

    private void selectNewXID(long uid) {
        u2x.remove(uid);
        List<Long> l = wait.get(uid);
        if (l == null) {
            return;
        }
        assert !l.isEmpty();
        while (!l.isEmpty()) {
            long xid = l.remove(0);
            if (waitLock.containsKey(xid)) {
                u2x.put(uid, xid);
                Lock lo = waitLock.remove(xid);
                waitU.remove(xid);
                lo.unlock();
                break;
            }
        }
        if (l.isEmpty()) {
            wait.remove(uid);
        }
    }

    private Map<Long, Integer> xidStamp;
    private int stamp;

    private boolean hasDeadLock() {
        xidStamp = new HashMap<>();
        stamp = 1;
        for (long xid : x2u.keySet()) {
            Integer s = xidStamp.get(xid);
            if (s != null && s > 0) {
                continue;
            }
            stamp++;
            if (dfs(xid)) {
                return true;
            }
        }
        return false;
    }

    private boolean dfs(long xid) {
        Integer stp = xidStamp.get(xid);
        if (stp != null && stp == stamp) {
            return true;
        }
        if (stp != null && stp < stamp) {
            return false;
        }
        xidStamp.put(xid, stamp);
        Long uid = waitU.get(xid);
        if (uid == null) {
            return false;
        }
        Long x = u2x.get(uid);
        assert x != null;
        return dfs(x);
    }

    private void removeFromList(Map<Long, List<Long>> listMap, long uid0, long uid1) {
        List<Long> l = listMap.get(uid0);
        if (l == null) {
            return;
        }
        Iterator<Long> it = l.iterator();
        while (it.hasNext()) {
            long e = it.next();
            if (e == uid1) {
                it.remove();
                return;
            }
        }
        if (l.isEmpty()) {
            listMap.remove(uid0);
        }
    }

    private void putIntoList(Map<Long, List<Long>> listMap, long uid0, long uid1) {
        if (!listMap.containsKey(uid0)) {
            listMap.put(uid0, new ArrayList<>());
        }
        listMap.get(uid0).add(0, uid1);
    }

    private boolean isInList(Map<Long, List<Long>> listMap, long uid0, long uid1) {
        List<Long> l = listMap.get(uid0);
        if (l == null) {
            return false;
        }
        for (long e : l) {
            if (e == uid1) {
                return true;
            }
        }
        return false;
    }
}
