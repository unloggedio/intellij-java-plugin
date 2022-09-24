package com.insidious.plugin.client;

import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.sql.SQLException;

class DaoServiceTest {

    private static ConnectionSource getConnection() throws SQLException {
        File dbFile = new File("execution.db");
        boolean dbFileExists = dbFile.exists();
//        dbFile.delete();
        // this uses h2 but you can change it to match your database
        String databaseUrl = "jdbc:sqlite:execution.db";
        // create a connection source to our database
        ConnectionSource connectionSource = new JdbcConnectionSource(databaseUrl);
        return connectionSource;
    }

    @Test
    void getMethodCallExpressionById() throws SQLException {

        DaoService daoService = new DaoService(getConnection());

        daoService.getMethodCallExpressionById(1L);

    }
}