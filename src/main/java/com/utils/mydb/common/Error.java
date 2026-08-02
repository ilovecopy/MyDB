package com.utils.mydb.common;


public class Error {


    private Error() {
        /* This utility class should not be instantiated */
    }
    //common
    public static final Exception CACHE_FULL_EXCEPTION = new Exception("Cache full.");
    public static final Exception FILE_EXIST_EXCEPTION = new Exception("File already exist.");
    public static final Exception FILE_NOT_EXIST_EXCEPTION = new Exception("File not exist.");
    public static final Exception FILE_CANNOT_READ_OR_WRITE_EXCEPTION = new Exception("File cannot read or write.");

    //dm
    public static final Exception BAD_LOG_FILE_EXCEPTION = new Exception("Bad log file.");
    public static final Exception MEMORY_TO_SMALL_EXCEPTION = new Exception("Memory is too small.");
    public static final Exception DATA_TOO_LARGE_EXCEPTION = new Exception("Data is too large.");
    public static final Exception DATABASE_BUSY_EXCEPTION = new Exception("Database is too busy.");

    //tm
    public static final Exception BAD_XID_FILE_EXCEPTION = new Exception("Bad xid file.");

    //vm
    public static final Exception DEAD_LOCK_EXCEPTION = new Exception("Dead lock detected");
    public static final Exception NULL_ENTRY_EXCEPTION = new Exception("Null entry");
    public static final Exception CONCURRENT_UPDATE_EXCEPTION = new Exception("Concurrent update exception.");

}
