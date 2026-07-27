package com.utils.mydb.backend.dm.tm;

import com.utils.mydb.backend.utils.Panic;
import com.utils.mydb.common.Error;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public interface TransactionManager {

    long begin();

    void commit(long xid);

    void abort(long xid);

    boolean isActive(long xid);

    boolean isCommitted(long xid);

    boolean isAborted(long xid);

    void close();

    static TransactionManagerImpl create(String path) {
        File f = new File(path + TransactionManagerImpl.XID_SUFFIX);
        try {
            if (!f.createNewFile()) {
                Panic.panic(Error.FILE_EXIST_EXCEPTION);
            }
        } catch (IOException e) {
            Panic.panic(e);
        }
        FileChannel fc = null;
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(f, "rw");
            fc = raf.getChannel();
        } catch (IOException e) {
            Panic.panic(e);
        }
        //写空XID文件头
        ByteBuffer buf = ByteBuffer.wrap(new byte[TransactionManagerImpl.XID_HEADER_SIZE]);
        try {
            fc.position(0);
            fc.write(buf);
        } catch (IOException e) {
            Panic.panic(e);
        }
        return new TransactionManagerImpl(raf, fc);
    }

    static TransactionManagerImpl open(String path) {
        File f = new File(path + TransactionManagerImpl.XID_SUFFIX);
        if (!f.exists()) {
            Panic.panic(Error.FILE_NOT_EXIST_EXCEPTION);
        }
        if (!f.canRead() || !f.canWrite()) {
            Panic.panic(Error.FILE_CANNOT_READ_OR_WRITE_EXCEPTION);
        }
        FileChannel fc = null;
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(f, "rw");
            fc = raf.getChannel();
        } catch (IOException e) {
            Panic.panic(e);
        }
        return new TransactionManagerImpl(raf, fc);
    }

}
