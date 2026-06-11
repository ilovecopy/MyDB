package com.utils.mydb.backend.dm.page;

import com.utils.mydb.backend.dm.pagecache.PageCache;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class PageImpl implements Page {
    /**
     * 当前页的页号，从1开始，对于文件中的物理位置
     */
    private final int pageNumber;
    /**
     * 8MB存储页面内容的字节数组
     */
    private final byte[] data;
    /**
     *脏页，true标识该页在内容中被修改过，与磁盘不一致
     */
    private boolean isDirty;
    private final Lock lock;
    /**
     *指向创建该页的页面缓存，用于释放页面时通知缓存
     */
    private final PageCache pageCache;

    public PageImpl(int pgno, byte[] data, PageCache pc) {
        this.pageNumber = pgno;
        this.data = data;
        this.pageCache = pc;
        this.lock = new ReentrantLock();

    }

    @Override
    public void lock() {
        lock.lock();
    }

    @Override
    public void unlock() {
        lock.unlock();
    }

    @Override
    public void release() {
        pageCache.release(this);
    }

    @Override
    public void setDirty(boolean dirty) {
        this.isDirty = dirty;
    }

    @Override
    public boolean isDirty() {
        return isDirty;
    }

    @Override
    public int getPageNumber() {
        return pageNumber;
    }

    @Override
    public byte[] getData() {
        return data;
    }
}
