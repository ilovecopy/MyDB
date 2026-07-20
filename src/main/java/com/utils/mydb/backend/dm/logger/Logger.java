package com.utils.mydb.backend.dm.logger;

import com.utils.mydb.backend.utils.Panic;
import com.utils.mydb.backend.utils.Parser;
import com.utils.mydb.common.Error;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public interface Logger {
    void log(byte[] data);

    void truncate(long x) throws Exception;

    byte[] next();

    void rewind();

    void close();

    static Logger create(String path) {
        File file = new File(path + LoggerImpl.LOG_SUFFIX);
        try {
            if (!file.createNewFile()) {
                Panic.panic(Error.FILE_EXIST_EXCEPTION);
            }
        } catch (Exception e) {
            Panic.panic(e);
        }
        if (!file.canRead() || !file.canWrite()) {
            Panic.panic(Error.FILE_CANNOT_READ_OR_WRITE_EXCEPTION);
        }
        FileChannel fc = null;
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(file, "rw");
            fc = raf.getChannel();
        } catch (FileNotFoundException e) {
            Panic.panic(e);
        }
        ByteBuffer buf = ByteBuffer.wrap(Parser.int2Byte(0));
        try {
            fc.position(0);
            fc.write(buf);
            fc.force(false);
        } catch (Exception e) {
            Panic.panic(e);
        }
        return new LoggerImpl(raf, fc, 0);
    }

    static Logger open(String path) {
        File file = new File(path + LoggerImpl.LOG_SUFFIX);
        if (!file.exists()) {
            Panic.panic(Error.FILE_NOT_EXIST_EXCEPTION);
        }
        if (!file.canRead() || !file.canWrite()) {
            Panic.panic(Error.FILE_CANNOT_READ_OR_WRITE_EXCEPTION);
        }
        FileChannel fc = null;
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(file, "rw");
            fc = raf.getChannel();
        } catch (FileNotFoundException e) {
            Panic.panic(e);
        }
        LoggerImpl logger = new LoggerImpl(raf, fc);
        logger.init();
        return logger;
    }

}
