package com.utils.mydb.backend.utils;

import com.google.common.primitives.Bytes;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class Parser {

    public static byte[] short2Byte(short value) {
        return ByteBuffer.allocate(Short.BYTES).putShort(value).array();
    }

    public static short parseShort(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getShort();
    }

    public static byte[] int2Byte(int value) {
        return ByteBuffer.allocate(Integer.BYTES).putInt(value).array();
    }

    public static int parseInt(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getInt();
    }

    public static byte[] long2Byte(long value) {
        return ByteBuffer.allocate(Long.BYTES).putLong(value).array();
    }

    public static long parseLong(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getLong();
    }

    public static ParseStringRes parseString(byte[] bytes) {
        int length = parseInt(Arrays.copyOf(bytes, Integer.BYTES));
        return new ParseStringRes(new String(Arrays.copyOfRange(bytes, Integer.BYTES, Integer.BYTES + length)),
                Integer.BYTES + length);
    }

    public static byte[] string2Bytes(String value) {
        byte[] bytes = int2Byte(value.length());
        return Bytes.concat(bytes, value.getBytes());
    }

    public static long str2Uid(String key) {
        long seed = 13331;
        long res = 0;
        for (byte b : key.getBytes()) {
            res = res * seed + b;
        }
        return res;
    }
}
