package com.geekbrains.brains.cloud.server;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class DBService {
    private static Connection connection;
    private static Statement stmt;
    private static Properties properties;

    public static void connect() throws SQLException {
        connection = DriverManager.getConnection(properties.getProperty("connectionString"), properties.getProperty("user"), properties.getProperty("password"));
        stmt = connection.createStatement();
    }

    public static boolean verify(String login, String pass) throws SQLException {
        String qry = String.format("SELECT User FROM cloud.users where user = '%s' and Password = '%s'", login, pass);
        ResultSet rs = stmt.executeQuery(qry);
        if (rs.next()) {
            return true;
        }
        return false;
    }

    public static Boolean register(String login, String pass)  {
        String qry = String.format("INSERT INTO cloud.users (User, Password) VALUES ('%s', '%s')", login, pass);
        try {
             stmt.execute(qry);
             return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void disconnect() {
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void setProperties(Properties properties) {
        DBService.properties = properties;
    }
}

