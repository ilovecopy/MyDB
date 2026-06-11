package com.utils.mydb.backend.utils;

public class Panic {
    public static void panic(Exception exception) {
        System.out.println(exception.getMessage());
        System.exit(1);
    }
}
