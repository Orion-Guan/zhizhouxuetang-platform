package com.tianji.learning.utils;


public class TableNameContext {

    private static final ThreadLocal<String> tableNameTL = new ThreadLocal<>();

    public static String getInfo(){
        return tableNameTL.get();
    }

    public static void setInfo(String tableName){
        tableNameTL.set(tableName);
    }

    public static void removeInfo(){
        tableNameTL.remove();
    }
}
