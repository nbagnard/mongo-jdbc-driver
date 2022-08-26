package com.mongodb.jdbc.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Properties;

public class MongoSQLTestUtils extends TestUtils {

    static final String URL = "jdbc:mongodb://localhost/?connectTimeoutms=600000";
    //public static final String MONGOSQL = "mongosql";
    public static final String TEST_DB = "tdvt";
    //public static final String TEST_DB = "integration_test";
    public static final String DEFAULT_TEST_COLLECTION = "test_collection";

    @Override
    protected Connection connect(Properties props) throws Exception {
        // Connects to local ADL instance
        if (props == null) {
            props = new Properties();
        }
        props.setProperty("User", System.getenv("ADL_TEST_LOCAL_USER"));
        props.setProperty("PassWord", System.getenv("ADL_TEST_LOCAL_PWD"));
        props.setProperty("authSource", System.getenv("ADL_TEST_LOCAL_AUTH_DB"));
        props.setProperty("Database", TEST_DB);
        props.setProperty("SsL", "false");
        props.setProperty("something", "something");
        props.setProperty("logLEvel", "SEVERE");

        System.out.println(props);
        Connection conn = DriverManager.getConnection(URL, props);

        try {
            Statement stmt = conn.createStatement();
            stmt.executeQuery("select 1 from " + DEFAULT_TEST_COLLECTION);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }

        return conn;
    }
}
