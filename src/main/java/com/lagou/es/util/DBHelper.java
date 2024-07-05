package com.lagou.es.util;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBHelper {
    public static final String url = "jdbc:mysql://221.219.96.222:13388/lagou_position?useSSL=false&useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai";
    public static final String name = "com.mysql.cj.jdbc.Driver";
    public static final String user = "root";
    public static final String password = "root";
    private  static Connection  connection = null;

    public  static   Connection  getConn(){
        try {
            Class.forName(name);
            connection = DriverManager.getConnection(url,user,password);
        }catch (Exception e){
            e.printStackTrace();
        }
        return  connection;
    }
}
