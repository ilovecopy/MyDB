package com.utils.mydb.backend.dm.pagecache;

import com.utils.mydb.backend.dm.page.Page;
import com.utils.mydb.backend.utils.Panic;
import com.utils.mydb.common.Error;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;

public interface PageCache {
    int PAGE_SIZE = 1 << 13;

    int newPage(byte[] initData);

    Page getPage(int pgno) throws Exception;

    void close();

    void release(Page page);

    void truncateByPageNo(int maxPgno);

    int getPageNumber();

    void flushPage(Page page);

    /**
     * 新建数据库文件db，并创建容量为memory/PAGE_SIZE的缓存
     *
     * @param path
     * @param memory
     * @return {@link PageCacheImpl }
     */
    static PageCacheImpl create(String path, long memory) {
        File f = new File(path + PageCacheImpl.DB_SUFFIX);
        try {
            if (!f.createNewFile()) {
                Panic.panic(Error.FILE_EXIST_EXCEPTION);
            }
        } catch (Exception e) {
            Panic.panic(e);
        }
        return getPageCache((int) memory, f);
    }

    /**
     * 打开已有数据库文件db，并创建容量为memory/PAGE_SIZE的缓存
     * @param path
     * @param memory
     * @return {@link PageCacheImpl }
     */
    static PageCacheImpl open(String path, long memory) {
        File f = new File(path + PageCacheImpl.DB_SUFFIX);
        if (!f.exists()) {
            Panic.panic(Error.FILE_NOT_EXIST_EXCEPTION);
        }
        return getPageCache((int) memory, f);
    }

    static PageCacheImpl getPageCache(int memory, File f) {
        if (!f.canRead() || !f.canWrite()) {
            Panic.panic(Error.FILE_CANNOT_READ_OR_WRITE_EXCEPTION);
        }
        FileChannel channel = null;
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(f, "rw");
            channel = raf.getChannel();
        } catch (FileNotFoundException e) {
            Panic.panic(e);
        }
        return new PageCacheImpl(raf, channel, memory / PAGE_SIZE);
    }
}
