package com.utils.mydb.backend.dm.pageindex;

import com.utils.mydb.backend.dm.pagecache.PageCache;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class PageIndex {
    private static final int INTERVALS_NO = 40;
    private static final int THRESHOLD = PageCache.PAGE_SIZE / INTERVALS_NO;

    private Lock lock;
    private List<PageInfo>[] lists;

    public PageIndex() {
        lock = new ReentrantLock();
        lists = new List[INTERVALS_NO + 1];
        for (int i = 0; i < lists.length; i++)
            lists[i] = new ArrayList<>();
    }

    public void add(int pgno, int freeSpace) {
        lock.lock();
        try {
            int index = freeSpace / THRESHOLD;
            lists[index].add(new PageInfo(pgno, freeSpace));
        } finally {
            lock.unlock();
        }
    }

    public PageInfo select(int freeSpace) {
        lock.lock();
        try {
            int index = freeSpace / THRESHOLD;
            if (index < INTERVALS_NO) {
                index++;
            }
            while (index <= INTERVALS_NO) {
                if (lists[index].isEmpty()) {
                    index++;
                    continue;
                }
                return lists[index].remove(0);
            }
            return null;
        } finally {
            lock.unlock();
        }
    }
}
