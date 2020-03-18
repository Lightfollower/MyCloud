package com.geekbrains.brains.cloud.server;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DBService {
    private static Connection connection;
    private static Statement stmt;

    public static void connect() throws SQLException {
//        try {
//            Class.forName("com.mysql.jdbc.Driver");
        connection = DriverManager.getConnection("jdbc:mysql://localhost:3306?serverTimezone=UTC", "root", "root");
        stmt = connection.createStatement();
//        } catch (ClassNotFoundException e) {
//            e.printStackTrace();
//        }
    }

    public static boolean verify(String login, String pass) throws SQLException {
        String qry = String.format("SELECT User FROM cloud.users where user = '%s' and Password = '%s'", login, pass);
        ResultSet rs = stmt.executeQuery(qry);

        if (rs.next()) {
            return true;
        }

        return false;
    }


    public static void disconnect() {
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static Boolean register(String login, String pass) throws SQLException {
        String qry = String.format("INSERT INTO cloud.users (User, Password) VALUES ('%s', '%s')", login, pass);
        return stmt.execute(qry);
    }
}

