package com.utils.mydb.backend.utils;

public class Types {

    /**
     * 页号32->64 位，页内偏移16->32位
     *
     * @param pgno
     * @param offset
     * @return long
     */
    public static long addressToUid(int pgno, short offset) {
        return ((long) pgno << 32) | offset;
    }
}
