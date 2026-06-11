package com.utils.mydb.backend.dm.pagecache;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.utils.mydb.backend.common.AbstractCache;
import com.utils.mydb.backend.dm.page.Page;
import com.utils.mydb.backend.dm.page.PageImpl;
import com.utils.mydb.backend.utils.Panic;
import com.utils.mydb.common.Error;

public class PageCacheImpl extends AbstractCache<Page> implements PageCache {
    private static final int MEM_PAGE_NUM = 10;
    public static final String DB_SUFFIX = ".db";

    private final RandomAccessFile raf;
    private final FileChannel fileChannel;
    private final Lock lock;
    private final AtomicInteger pageNumbers;

    public PageCacheImpl(RandomAccessFile file, FileChannel channel, int maxResource) {
        super(maxResource);
        if (maxResource < MEM_PAGE_NUM) {
            Panic.panic(Error.MEMORY_TO_SMALL_EXCEPTION);
        }
        long length = 0;
        try {
            length = file.length();
        } catch (Exception e) {
            Panic.panic(e);
        }
        this.raf = file;
        this.fileChannel = channel;
        this.lock = new ReentrantLock();
        this.pageNumbers = new AtomicInteger((int) (length / PAGE_SIZE));
    }

    /**
     * 读者要在书末尾加一张空白页
     *
     * @param initData
     * @return int
     */
    @Override
    public int newPage(byte[] initData) {
        int pgno = pageNumbers.incrementAndGet();
        Page page = new PageImpl(pgno, initData, null);
        flush(page);
        return pgno;
    }

    /**
     * 强行把工作台某一页内容抄回书里
     *
     * @param page
     */
    private void flush(Page page) {
        int pgno = page.getPageNumber();
        long offset = pageOffset(pgno);
        lock.lock();
        try {
            ByteBuffer buffer = ByteBuffer.wrap(page.getData());
            fileChannel.position(offset);
            fileChannel.write(buffer);
            fileChannel.force(true);
        } catch (IOException e) {
            Panic.panic(e);
        } finally {
            lock.unlock();
        }
    }

    private static long pageOffset(int pgno) {
        return (long) (pgno - 1) * PAGE_SIZE;
    }

    @Override
    public Page getPage(int pgno) throws Exception {
        return get(pgno);
    }

    /**
     *图书馆下班
     */
    @Override
    public void close() {
        super.close();//所有改过的页全部写回书
        try {
            raf.close();//合上书
            fileChannel.close();
        } catch (IOException e) {
            Panic.panic(e);
        }
    }

    @Override
    public void release(Page page) {
        release(page.getPageNumber());
    }

    /**
     * 从某页截断，截断后，所有页号大于 maxPgno 的页，全部被删除
     *
     * @param maxPgno
     */
    @Override
    public void truncateByPageNo(int maxPgno) {
        long size = pageOffset(maxPgno + 1);
        try {
            raf.setLength(size);
        } catch (IOException e) {
            Panic.panic(e);
        }
        pageNumbers.set(maxPgno);
    }

    /**
     * 书一共有多少页
     * @return int
     */
    @Override
    public int getPageNumber() {
        return pageNumbers.intValue();
    }

    @Override
    public void flushPage(Page page) {
        flush(page);
    }

    /**
     * 工作台没这页，从磁盘拿
     *
     * @param key
     * @return {@link Page }
     */
    @Override
    protected Page getForCache(long key) {
        int pgno = (int) key;
        long offset = pageOffset(pgno);
        ByteBuffer buffer = ByteBuffer.allocate(PAGE_SIZE);
        lock.lock();
        try {
            fileChannel.position(offset);
            fileChannel.read(buffer);
        } catch (IOException e) {
            Panic.panic(e);
        } finally {
            lock.unlock();
        }
        return new PageImpl(pgno, buffer.array(), this);
    }

    /**
     * 要扔掉工作台上的一页
     *
     * @param pg
     */
    @Override
    protected void releaseForCache(Page pg) {
        if (pg.isDirty()) {
            flush(pg);
            pg.setDirty(false);
        }
    }
}
