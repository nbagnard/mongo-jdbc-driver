/*
 * Copyright 2024-present MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mongodb.jdbc.integration;

import static org.junit.jupiter.api.Assertions.*;

import com.mongodb.jdbc.MongoConnection;
import com.mongodb.jdbc.MongoDatabaseMetaData;
import com.mongodb.jdbc.Pair;
import java.sql.*;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DCIntegrationTest {

    /**
     * Connect to a remote cluster to use for the tests.
     *
     * @return the connection to the enterprise cluster to use for the tests.
     * @throws SQLException If the connection failed.
     */
    private Connection remoteTestInstanceConnect() throws SQLException {
        String mongoHost = System.getenv("SRV_TEST_HOST");
        assertNotNull(mongoHost, "SRV_TEST_HOST variable not set in environment");
        String mongoURI =
                "mongodb+srv://"
                        + mongoHost
                        + "/?readPreference=secondaryPreferred&connectTimeoutMS=300000";
        String fullURI = "jdbc:" + mongoURI;

        String user = System.getenv("SRV_TEST_USER");
        assertNotNull(user, "SRV_TEST_USER variable not set in environment");
        String pwd = System.getenv("SRV_TEST_PWD");
        assertNotNull(pwd, "SRV_TEST_PWD variable not set in environment");
        String authSource = System.getenv("SRV_TEST_AUTH_DB");
        assertNotNull(authSource, "SRV_TEST_AUTH_DB variable not set in environment");

        Properties p = new java.util.Properties();
        p.setProperty("user", user);
        p.setProperty("password", pwd);
        p.setProperty("authSource", authSource);
        p.setProperty("database", "test");

        return DriverManager.getConnection(fullURI, p);
    }

    /** Tests that the driver can work with SRV-style URIs. */
    @Test
    public void testConnectWithSRVURI() throws SQLException {
        try (Connection conn = remoteTestInstanceConnect(); ) {
            // Let's use the connection to make sure everything is working fine.
            conn.getMetaData().getDriverVersion();
        }
    }

    /**
     * Gets information from the environment to create a connection to a local mongod.
     *
     * @param typeEnvVar Either "MDB_LOCAL_PORT_COM" or "MDB_LOCAL_PORT_ENT"
     * @return A (jdbc_uri, properties) pair with which to create a MongoConnection
     */
    private Pair<String, Properties> createLocalMongodConnInfo(String typeEnvVar) {
        String mongoPort = System.getenv(typeEnvVar);
        assertNotNull(mongoPort, typeEnvVar + " variable not set in environment");

        String uri = "jdbc:mongodb://localhost:" + mongoPort + "/test";

        String user = System.getenv("LOCAL_MDB_USER");
        assertNotNull(user, "LOCAL_MDB_USER variable not set in environment");
        String pwd = System.getenv("LOCAL_MDB_PWD");
        assertNotNull(pwd, "LOCAL_MDB_PWD variable not set in environment");

        Properties p = new java.util.Properties();
        p.setProperty("user", user);
        p.setProperty("password", pwd);
        p.setProperty("authSource", "test");
        p.setProperty("database", "test");

        return new Pair<>(uri, p);
    }

    /** Tests that the driver rejects the community edition of the server. */
    @Test
    public void testConnectionToCommunityServerFails() {
        Pair<String, Properties> info = createLocalMongodConnInfo("LOCAL_MDB_PORT_COM");
        try (MongoConnection conn =
                (MongoConnection) DriverManager.getConnection(info.left(), info.right()); ) {
            assertThrows(java.sql.SQLException.class, () -> {});

        } catch (SQLException e) {
            assertTrue(
                    e.getCause().getMessage().contains("Community edition detected"),
                    e.getCause().getMessage() + " doesn't contain \"Community edition detected\"");
        }
    }

    /** Tests that the driver connects to the enterprise edition of the server. */
    @Test
    public void testConnectionToEnterpriseServerSucceeds() throws SQLException {
        Pair<String, Properties> info = createLocalMongodConnInfo("LOCAL_MDB_PORT_ENT");
        try (Connection conn = DriverManager.getConnection(info.left(), info.right()); ) {
            // Let's use the connection to make sure everything is working fine.
            conn.getMetaData().getDriverVersion();
        }
    }

    @Test
    public void testInvalidQueryShouldFail() throws SQLException {
        try (Connection conn = remoteTestInstanceConnect();
                Statement stmt = conn.createStatement(); ) {
            // Invalid SQL query should fail
            assertThrows(
                    java.sql.SQLException.class,
                    () -> {
                        try {
                            stmt.executeQuery("This is not valid SQL");
                        } catch (SQLException e) {
                            // Let's make sure that we fail for the reason we expect it to.
                            assert (e.getMessage().contains("Error 2001"));
                            throw e;
                        }
                    });
        }
    }

    @Test
    public void testValidSimpleQueryShouldSucceed() throws SQLException {
        try (Connection conn = remoteTestInstanceConnect();
                Statement stmt = conn.createStatement(); ) {
            ResultSet rs = stmt.executeQuery("SELECT * from accounts");
            assert (rs.next());
            // Let's just check that we can access the data and don't blow up.
            rs.getString(1);
            rs.close();
        }
    }

    @Test
    public void testCollectionLessQueryShouldSucceed() throws SQLException {
        try (Connection conn = remoteTestInstanceConnect();
                Statement stmt = conn.createStatement(); ) {
            ResultSet rs = stmt.executeQuery("SELECT 1");
            assert (rs.next());
            // Let's just check that we can access the data and don't blow up.
            rs.getString(1);
            rs.close();
        }
    }

    @Test
    public void testValidSimpleQueryNoSchemaForCollectionShouldSucceed() throws SQLException {
        try (Connection conn = remoteTestInstanceConnect();
                Statement stmt = conn.createStatement(); ) {
            ResultSet rs = stmt.executeQuery("SELECT account_id from acc_limit_over_1000 limit 5");
            assert (rs.next());
            // Let's just check that we can access the data and don't blow up.
            rs.getString(1);
            rs.close();
        }
    }

    @Test
    public void testListDatabasse() throws SQLException {
        try (Connection conn = remoteTestInstanceConnect(); ) {
            ResultSet rs = conn.getMetaData().getCatalogs();
            while (rs.next()) {
                // Verify that none of the system databases are returned
                assert (!MongoDatabaseMetaData.DISALLOWED_DB_NAMES
                        .matcher(rs.getString(1))
                        .matches());
            }
            rs.close();
        }
    }

    @Test
    public void testListTables() throws SQLException {
        try (Connection conn = remoteTestInstanceConnect(); ) {
            ResultSet rs = conn.getMetaData().getTables(null, null, "%", null);
            while (rs.next()) {
                // Verify that none of the system collections are returned
                assert (!MongoDatabaseMetaData.DISALLOWED_COLLECTION_NAMES
                        .matcher(rs.getString(3))
                        .matches());
            }
            rs.close();
        }
    }

    @Test
    public void testColumnsMetadataForCollectionWithSchema() throws SQLException {
        try (Connection conn = remoteTestInstanceConnect(); ) {
            ResultSet rs = conn.getMetaData().getColumns(null, null, "accounts", "%");
            assert (rs.next());
            // Let's just check that we can access the data and don't blow up.
            rs.getString(1);
            rs.close();
        }
    }

    @Test
    public void testColumnsMetadataForCollectionWithNoSchema() throws SQLException {
        try (Connection conn = remoteTestInstanceConnect(); ) {
            ResultSet rs = conn.getMetaData().getColumns(null, null, "acc_limit_over_1000", "%");
            // Let's just check that the result set is empty and we don't blow up.
            assert (!rs.next());
            rs.close();
        }
    }
}
