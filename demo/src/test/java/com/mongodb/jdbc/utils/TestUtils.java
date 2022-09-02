package com.mongodb.jdbc.utils;

import com.google.common.io.BaseEncoding;
import com.mongodb.jdbc.MongoResultSet;
import com.mongodb.jdbc.Pair;
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import java.math.BigDecimal;
import java.net.Inet4Address;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/***
 * This class provides the baseline for common things that applications built on top of JDBC use.
 * - Connecting to the database: testConnection
 * - Executing a query and printing out the result set information: testExecute, testExecutePrintMetadata.
 * - Retrieving database metadata information to check the driver information and capability: testDBMeta.
 * - Retrieving catalog information: testGetCatalogs, testGetSchemas, testGetTables, testGetTablePrivileges...
 * - Cancelling a long running query: testCancel
 *
 * It does not cover everything an application can/will do (there is no prepare/execute flow for example).
 *
 * It is expected that you will modify the code here based on your need when you want to try out or debug something.
 * The version in the GitHub repository is only a first minimalistic version to help you get started.
 * This class must live and its content evolve with you.
 *
 * Please note that in order to run one of the helper you need to uncomment the "exclude 'com/mongodb/jdbc/utils/**'" line
 * in the build.gradle file.
 */
public abstract class TestUtils {

    protected enum RsPrintType {
        VALUES,
        METADATA,
        JSON_SCHEMA
    }

    static final String LOG_LEVEL = "LogLevel";
    static final String LOG_DIR = "LogDir";

    static final HashSet VALUES_ONLY = new HashSet<>();
    static final HashSet METADATA_ONLY = new HashSet<>();
    static final HashSet JSONSCHEMA_ONLY = new HashSet<>();
    static final HashSet METADATA_AND_VALUES = new HashSet<>();
    static final HashSet ALL = new HashSet<>();
    static {
        VALUES_ONLY.add(RsPrintType.VALUES);
        METADATA_ONLY.add(RsPrintType.METADATA);
        JSONSCHEMA_ONLY.add(RsPrintType.JSON_SCHEMA);
        METADATA_AND_VALUES.add(RsPrintType.METADATA);
        METADATA_AND_VALUES.add(RsPrintType.VALUES);
        ALL.add(RsPrintType.VALUES);
        ALL.add(RsPrintType.METADATA);
        ALL.add(RsPrintType.JSON_SCHEMA);
    }

    protected Connection m_conn;
    protected DatabaseMetaData m_dbMeta;

    protected abstract Connection connect(Properties props) throws Exception;

    @BeforeEach
    private void execConnection() throws Exception {
        Properties props = new Properties();
        //props.setProperty(LOG_LEVEL, Level.FINER.getName());
        m_conn = connect(props);
        m_dbMeta = m_conn.getMetaData();
    }

    @AfterEach
    private void execDeconnection() throws Exception {
        m_conn.close();
    }

    @Test
    void closeOnCompletion() throws SQLException {
        Statement stmt = m_conn.createStatement();
        stmt.closeOnCompletion();
        ResultSet rs = stmt.executeQuery("Select 1");
        stmt.close();
    }

    @Test
    void tryLogging() throws Exception {
        runExecuteQuery("select * from bar",VALUES_ONLY);
        //((MongoConnection) m_conn).getConnectionId();
        try {
            Properties props = new Properties();
            Connection conn2 = connect(props);
            //((MongoConnection) conn2).getConnectionId();
            Statement stmt = conn2.createStatement();
            stmt.executeQuery("Invalid SQL");
            stmt.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        try {
            Properties props = new Properties();
            props.setProperty(LOG_LEVEL, Level.SEVERE.getName());
            Connection conn3 = connect(props);
            //((MongoConnection) conn3).getConnectionId();
            Statement stmt = conn3.createStatement();
            stmt.executeQuery("Another invalid SQL");
            stmt.close();
        }catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Test
    void tryConnecting() throws Exception {
        System.out.println(m_dbMeta.getDriverName());
        System.out.println(m_dbMeta.getDriverVersion());

        System.out.println("Connected !");
    }

    @Test
    void testBase64()
    {
        String text = "toto à la plage";
        byte[] data = text.getBytes(StandardCharsets.UTF_8);
        System.out.println(Base64.encode(data));
    }

    @Test
    void tryGetBytes() throws Exception {
        try {
            Statement stmt = m_conn.createStatement();
            ResultSet rs = stmt.executeQuery("select * from binaryData");
            ResultSetMetaData rsMeta = rs.getMetaData();
            while (rs.next()){
                    System.out.println("ID " + rs.getString(1));
                    byte[] data = rs.getBytes(2);
                    System.out.println(ArrayUtils.toString(data));
                    String base64 = Base64.encode(data);
                    System.out.println(ArrayUtils.toString(base64));
                    String str = new String(data, StandardCharsets.UTF_8);
                    System.out.println("getString = " + rs.getString(2));
                    System.out.println("getObject.toString = " + rs.getObject(2).toString());
                    System.out.println("Hex string = " + BaseEncoding.base16().upperCase().encode(data));

                    String string = new String(Base64.decode(base64), "UTF-8");
                    System.out.println("Decoded UTF-8 string = " + string);
                    System.out.println("----------------------------------");
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    void runQuery() throws Exception {

        //runExecuteQuery("SELECT SPLIT('str-tgf-hdjkf', '-', 2)", true);
        runExecuteQuery("SELECT num0,num2,num3,num4, (CASE WHEN (num0 > 3000) THEN num2 WHEN NOT (num0 > 3000) THEN num3 ELSE num4 END) from Calcs", VALUES_ONLY);

    }

    @Test
    void runINLiteral() throws Exception {

        runExecuteQuery("SELECT str2, key, int1 FROM Calcs", VALUES_ONLY);
        runExecuteQuery("SELECT str2, key, int1 FROM Calcs WHERE int1 IN (3)", VALUES_ONLY);

    }

    @Test
    void testMIN() throws Exception {
        //runExecuteQuery("SELECT int0, int1, (CASE WHEN (\"Calcs\".\"int0\" < \"Calcs\".\"int1\") THEN \"Calcs\".\"int0\" ELSE \"Calcs\".\"int1\" END) AS \"MinVal\" FROM Calcs group by MinVal", true);
        runExecuteQuery("SELECT int0, int1, (CASE WHEN (\"Calcs\".\"int0\" > \"Calcs\".\"int1\") THEN \"Calcs\".\"int0\" ELSE \"Calcs\".\"int1\" END) AS \"MaxVal\" FROM Calcs", VALUES_ONLY);
        //runExecuteQuery("SELECT int0, int1, SPLIT(str0, ' ', 1) as gpVal FROM Calcs group by gpVal", true);
    }

    @Test
    void testDateLiteralAsInt() throws Exception {

        runExecuteQuery("SELECT CAST(date AS DOUBLE) as toto FROM Calcs group by toto", VALUES_ONLY);
        //runExecuteQuery("SELECT '$__alias__1' FROM Calcs", true);

    }

    //db.aggregate([{$sql: {statement: "SELECT * FROM (SELECT SUM(\"Calcs\".\"num1\") AS \"__measure__0\" FROM \"Calcs\" HAVING (COUNT(1) > 0)) \"t0\" CROSS JOIN (SELECT AVG(\"t1\".\"__measure__0\") AS \"__measure__1\" FROM (SELECT SUM(\"Calcs\".\"num1\") AS \"__measure__0\" FROM \"Calcs\" GROUP BY \"Calcs\".\"str1\") \"t1\" HAVING (COUNT(1) > 0)) \"t2\"", format: "jdbc", formatVersion: 1, dialect: "mongosql"}}])
    @Test
    void testCrossJoin() throws Exception {

        runExecuteQuery("SELECT SUM(\"Calcs\".\"num1\") AS \"__measure__0\"\n" +
                "  FROM \"Calcs\"\n" +
                "  HAVING (COUNT(1) > 0)", null);

        runExecuteQuery("SELECT * FROM (SELECT SUM(\"Calcs\".\"num1\") AS \"__measure__0\"\n" +
                "  FROM \"Calcs\"\n" +
                "  HAVING (COUNT(1) > 0)) t0", null);

        runExecuteQuery("SELECT __measure__0 FROM (SELECT SUM(\"Calcs\".\"num1\") AS \"__measure__0\"\n" +
                "  FROM \"Calcs\"\n" +
                "  HAVING (COUNT(1) > 0)) t0", null);

        runExecuteQuery("SELECT * FROM (SELECT (\"Calcs\".\"num1\") AS \"__measure__0\"\n" +
                "  FROM \"Calcs\") t0", null);

    }

    // db.aggregate([{$sql: {statement: "SELECT SUM(\"calcs\".\"num1\") AS \"__measure_0\" FROM \"calcs\" GROUP BY \"calcs\".\"str1\"", format: "jdbc", formatVersion: 1, dialect: "mongosql"}}])
    @Test
    void testGroupBy() throws Exception {

        // OK
        /*
        +----------------------|----------------------+
        | _measure_0           | str1                 |
        +----------------------|----------------------+
        | 10.32                | CD-R MEDIA           |
        | 9.47                 | ANSWERING MACHINES   |
        | 7.1                  | DOT MATRIX PRINTERS  |
        | 6.71                 | CLOCKS               |
        | 12.4                 | BUSINESS COPIERS     |
        | 10.37                | CORDLESS KEYBOARDS   |
        | 9.05                 | BINDER CLIPS         |
        | 12.05                | CORDED KEYBOARDS     |
        | 8.42                 | CLAMP ON LAMPS       |
        | 16.42                | BINDING SUPPLIES     |
        | 7.12                 | ERICSSON             |
        | 11.38                | BUSINESS ENVELOPES   |
        | 16.81                | DVD                  |
        | 9.78                 | AIR PURIFIERS        |
        | 9.38                 | BINDING MACHINES     |
        | 2.47                 | CONFERENCE PHONES    |
        | 7.43                 | BINDER ACCESSORIES   |
        +----------------------|----------------------+
         */
        runExecuteQuery("SELECT \"Calcs\".\"str1\", SUM(\"Calcs\".\"num1\") AS \"_measure_0\" FROM \"Calcs\" GROUP BY \"Calcs\".\"str1\"", VALUES_ONLY);

        // OK
        /*
        +----------------------+
        | __measure__0         |
        +----------------------+
        | 166.68               |
        +----------------------+
         */
        runExecuteQuery("SELECT SUM(\"Calcs\".\"num1\") AS \"__measure__0\"\n" +
                "  FROM \"Calcs\"\n" +
                "  HAVING (COUNT(1) > 0)\n", VALUES_ONLY);

        // OK
        /*
        +----------------------+
        | __measure__1         |
        +----------------------+
        | 9.804705882352941    |
        +----------------------+
         */
        runExecuteQuery("SELECT AVG(\"t1\".\"__measure__0\") AS \"__measure__1\"\n" +
                "  FROM (\n" +
                "    SELECT SUM(\"Calcs\".\"num1\") AS \"__measure__0\"\n" +
                "    FROM \"Calcs\"\n" +
                "    GROUP BY \"Calcs\".\"str1\"\n" +
                "  ) \"t1\"\n" +
                "  HAVING (COUNT(1) > 0)", VALUES_ONLY);

        /*
        +----------------------|----------------------+
        | __measure__0         | __measure__1         |
        +----------------------|----------------------+
        +----------------------|----------------------+
         */
        runExecuteQuery(
                // 166.68
                " SELECT * FROM (SELECT SUM(\"Calcs\".\"num1\") AS \"__measure__0\"\n" +
                "  FROM \"Calcs\"\n" +
                "  HAVING (COUNT(1) > 0)\n" +
                ") \"t0\"\n" +
                "  CROSS JOIN (\n" +
                // OK
                "  SELECT AVG(\"t1\".\"__measure__0\") AS \"__measure__1\"\n" +
                "  FROM (\n" +
                // OK
                "    SELECT SUM(\"Calcs\".\"num1\") AS \"__measure__0\"\n" +
                "    FROM \"Calcs\"\n" +
                "    GROUP BY \"Calcs\".\"str1\"\n" +
                "  ) \"t1\"\n" +
                "  HAVING (COUNT(1) > 0)\n" +
                ") \"t2\"", VALUES_ONLY);

        runExecuteQuery("SELECT (\"t0\".\"__measure__0\" / \"t2\".\"__measure__1\") AS \"TEMP(Test)(3502400386)(0)\"\n" +
                "FROM (\n" +
                // 166.68
                "  SELECT SUM(\"Calcs\".\"num1\") AS \"__measure__0\"\n" +
                "  FROM \"Calcs\"\n" +
                "  HAVING (COUNT(1) > 0)\n" +
                ") \"t0\"\n" +
                "  CROSS JOIN (\n" +
                // 9.804705882352941
                "  SELECT AVG(\"t1\".\"__measure__0\") AS \"__measure__1\"\n" +
                "  FROM (\n" +
                    // OK
                "    SELECT SUM(\"Calcs\".\"num1\") AS \"__measure__0\"\n" +
                "    FROM \"Calcs\"\n" +
                "    GROUP BY \"Calcs\".\"str1\"\n" +
                "  ) \"t1\"\n" +
                "  HAVING (COUNT(1) > 0)\n" +
                ") \"t2\"", VALUES_ONLY);
    }

    @Test
    void runInTest() throws Exception {
        try {

            //runExecuteQuery("SELECT 'six'", false);
            //runExecuteQuery("SELECT 'six'", true);

            //runExecuteQuery("SELECT subCalcs.str2 as substr2 from Calcs as subCalcs where subCalcs.str2 = 'six'", false);
            //runExecuteQuery("SELECT subCalcs.str2 as substr2 from Calcs as subCalcs where subCalcs.str2 = 'six'", true);

            //runExecuteQuery("SELECT Calcs.str2, Calcs.key,Calcs.int1 FROM Calcs AS Calcs WHERE Calcs.str2 IN (SELECT subCalcs.str2 as _six from Calcs as subCalcs WHERE subCalcs.str2 = 'six')", true);

            //runExecuteQuery("SELECT VALUE {'str2': Calcs.str2, 'key': Calcs.key, 'int1': Calcs.int1} FROM Calcs AS Calcs WHERE Calcs.str2 = ANY(SELECT 'six')", true);


            //runExecuteQuery("SELECT Calcs.str2,Calcs.key, Calcs.int1  FROM Calcs WHERE Calcs.int1 IN (SELECT subCalcs.int1 as subint1 from Calcs as subCalcs where subCalcs.int1 = 3)", true);


            //runExecuteQuery("SELECT VALUE {'str2': Calcs.str2, 'key': Calcs.key, 'int1': Calcs.int1} FROM Calcs AS Calcs WHERE Calcs.str2 = ANY(SELECT subCalcs.str2 as _six from Calcs as subCalcs WHERE subCalcs.str2 = 'six')", true);


            //runExecuteQuery("SELECT * FROM Calcs WHERE Calcs.int1 IN (SELECT subCalcs.int1 as subint1 from Calcs as subCalcs where subCalcs.int1 = 3 OR  subCalcs.int1 = 6)", true);
            runExecuteQuery("SELECT * FROM Calcs WHERE Calcs.int1 IN (SELECT _1 from [{'_1': 3}, {'_1': 6}] AS _arr)", VALUES_ONLY);

            //runExecuteQuery("SELECT VALUE {'_1': _1} FROM [{'_1': 3}, {'_1': 6}] AS _arr", false);
            //runExecuteQuery("SELECT VALUE {'_1': _1} FROM [{'_1': 3}, {'_1': 6}] AS _arr", true);


            //runExecuteQuery("SELECT * FROM Calcs AS Calcs WHERE Calcs.int1 = ANY(SELECT VALUE {'_1': _1} FROM [{'_1': 3}, {'_1': 6}] AS _arr)", true);


            //runExecuteQuery("SELECT VALUE {'a': a} FROM [{'a': 1, 'b': 1}] AS arr", false);

            //runExecuteQuery("SELECT VALUE {'a': a} FROM [{'a': 1, 'b': 1}] AS arr", true);


            //runExecuteQuery("SELECT VALUE {'str2': Calcs.str2, 'key': Calcs.key, 'int1': Calcs.int1} FROM Calcs AS Calcs WHERE Calcs.int1 = ANY(SELECT VALUE _1 FROM [{'_1': 3}] AS _arr)", true);


            //runExecuteQuery("SELECT VALUE {'str2': Calcs.str2, 'key': Calcs.key, 'int1': Calcs.int1} FROM Calcs AS Calcs WHERE Calcs.int1 = ANY(SELECT VALUE _1 FROM [{'_1': 3}] AS _arr)", true);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void tryExecute() throws Exception {
        try {
            //runExecuteQuery("SELECT str2, (CASE WHEN (str2 IN ('eleven', 'fifteen', 'five', 'fourteen', 'nine', 'one', 'six', 'sixteen', 'ten', 'three', 'twelve')) THEN 'InSet' ELSE str2 END) AS \"Str2Gp\" FROM Calcs", VALUES_ONLY);
           // runExecuteQuery("SELECT (CASE WHEN (str2 IN ('eleven', 'fifteen', 'five', 'fourteen', 'nine', 'one', 'six', 'sixteen', 'ten', 'three', 'twelve')) THEN 'InSet' ELSE str2 END) AS \"Str2Gp\" FROM Calcs group by Str2Gp", VALUES_ONLY);


            /*

            select
	  s.stock.plantname plantname ,
	SUM(s.stock.quantity) quantity,
	SUM( s.stock.quantity * s.stock.price / fx.rate) total,
	MAX(s.stock.price / fx.rate) as mostexpensive
from
	UNWIND(LeafyData.Sellers s,
	fxrates fx WITH PATH => stock )
WHERE
	s.currency = fx.currency
group by
	plantname
order by
	plantname
             */

            //SELECT * from unwind(Transactions t, customers c with path => c.accounts) where t.account_id = c.accounts ==> 13570ms

            /*
                    runExecuteQuery("SELECT transactions.`symbol` as symb, SUM(transactions.price::FLOAT) from unwind(Transax t, Accounts c with path => transactions) where t.account_id = c.account_id group by symb"
                    ,VALUES_ONLY); ==> 55803ms

                     runExecuteQuery("SELECT transactions.`symbol` as symb, SUM(transactions.price::FLOAT/`limit`) from unwind(Transax t, Accounts c with path => transactions) where t.account_id = c.account_id group by symb"
                    ,VALUES_ONLY); ==> 51820ms
                    runExecuteQuery("SELECT transactions.`symbol` as symb, SUM(transactions.price::FLOAT/`limit`) from unwind((SELECT t.transactions, c.`limit` FROM Transax t, Accounts c where t.account_id = c.account_id) sub with path => sub.transactions) group by symb"
                    ,ALL); ==> 52137ms
             */

            //runExecuteQuery("SELECT joined.transactions.`symbol`, SUM(joined.price/`limit`) FROM ((SELECT t.account_id, t.transactions.`symbol`, t.price, c.`limit` from unwind(Transax t with path => transactions)) sub, Accounts c where sub.account_id = c.account_id) joined", VALUES_ONLY);
                         runExecuteQuery("select ProductSold from Transactions group by ProductSold", VALUES_ONLY);


//            runExecuteQuery(" SELECT key, date0 " +
//            "FROM \"Calcs\"\n" +
//            " WHERE (\"Calcs\".\"date0\" IN (select subCalcs.date0 from Calcs as subCalcs where subCalcs.date0 = (CAST('1972-07-04 00:00:00.000' AS TIMESTAMP)) OR subCalcs.date0 = (CAST('1975-11-12 00:00:00.000' AS TIMESTAMP)) OR subCalcs.date0 = (CAST('2004-06-19 00:00:00.000' AS TIMESTAMP))))\n" +
//
//            //        " WHERE (\"Calcs\".\"date0\" IN (CAST('1972-07-04 00:00:00.000' AS TIMESTAMP), CAST('1975-11-12 00:00:00.000' AS TIMESTAMP), CAST('2004-06-19 00:00:00.000' AS TIMESTAMP)))\n" +
//            /*"GROUP BY \"key\""*/
//            ""
//            , ALL);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        /*
        try {
            //  "t0"."__measure__0",  "t2"."__measure__1", ("t0"."__measure__0" / "t2"."__measure__1") AS "TEMP(Test)(3502400386)(0)"
            runExecuteQuery(" SELECT CAST(2050-01-01 AS DOUBLE) AS \"TEMP(Test)(3947742720)(0)\"\n" +
                            "FROM \"Calcs\"\n" +
                            "HAVING (COUNT(1) > 0)"
                    , true);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
         */



                /*
        "SELECT AVG(\"t1\".\"__measure__0\") AS \"__measure__1\"\n" +
                            "  FROM (\n" +
                            "    SELECT \"Calcs\".\"str1\", SUM(\"Calcs\".\"num1\") AS \"__measure__0\"\n" +
                            "    FROM \"Calcs\"\n" +
                            "    GROUP BY \"Calcs\".\"str1\"\n" +
                            "  ) \"t1\"\n" +
                            "  HAVING (COUNT(1) > 0)\n"
                           -->
                           __measure__1 = 9.804705882352941

         "SELECT SUM(\"Calcs\".\"num1\") AS \"__measure__0\"\n" +
                            "  FROM \"Calcs\"\n" +
                            "  HAVING (COUNT(1) > 0)"
                            __measure__0  = 166.68
         */
    }

    /*
CREATE TABLE test (
  id INT,
  strVal VARCHAR(250)
);
INSERT INTO test VALUES (0, 'totoalaplage');
INSERT INTO test VALUES (1, '');
INSERT INTO test VALUES (2, NULL);
INSERT INTO test VALUES (3, 'a');
INSERT INTO test VALUES (4, 'b');
INSERT INTO test VALUES (5, 'ab');
INSERT INTO test VALUES (6, 'ba');
INSERT INTO test VALUES (7, 'ala');
     */
    @Test
    void tryFINDStringSubstringStart() throws Exception {
        // SELECT _id, strVal, POSITION('la' IN SUBSTRING(strVal FROM (2-1) FOR CHAR_LENGTH(strVal))) from test_string_collection
        String queryPattern = "SELECT '%s' as `sub_start`, '%s' as `sub_find`, _id, strVal, (CASE WHEN pos >= 0 THEN (pos + %d) ELSE 0 END) as idx, (CASE WHEN pos >= 0 THEN (SUBSTRING(strVal FROM (pos + %d -1) FOR CHAR_LENGTH(strVal))) ELSE NULL END) as subst from (SELECT _id, strVal, (POSITION('%s' IN SUBSTRING(strVal FROM (%d-1) FOR CHAR_LENGTH(strVal)))) pos FROM test_string_collection) as test_string_collection_pos";
        List<Pair<String, Integer>> tests = new ArrayList<>();
        tests.add(new Pair<String, Integer>("la", 1));
        tests.add(new Pair<String, Integer>("la", 2));
        tests.add(new Pair<String, Integer>("la", 3));
        tests.add(new Pair<String, Integer>("la", 6));
        tests.add(new Pair<String, Integer>("la", 7));
        tests.add(new Pair<String, Integer>("la", 20));
        tests.add(new Pair<String, Integer>("a", 1));
        tests.add(new Pair<String, Integer>("a", 2));
        for (Pair<String, Integer> elem : tests) {
            String query = String.format(queryPattern, elem.right(), elem.left(), elem.right(), elem.right(), elem.left(), elem.right());
            System.out.println("Query " + query);
            runExecuteQuery(query, VALUES_ONLY);
        }

        String query = "SELECT _id, strVal, (POSITION('lo' IN SUBSTRING(strVal FROM 3 FOR 2))) pos FROM test_string_collection";
        System.out.println("Query " + query);
        runExecuteQuery(query, VALUES_ONLY);
    }
    @Test
    void tryLEFTStringNumber() throws Exception {
        String queryPattern = "SELECT _id, strVal, '%s' as `number`, (SUBSTRING(strVal FROM 0 FOR %d)) as `left` FROM test_string_collection";
        int[] tests = new int[] {0,1,2,3,4,5,20};

        for (int elem : tests) {
            String query = String.format(queryPattern, elem, elem);
            System.out.println("Query " + query);
            runExecuteQuery(query, VALUES_ONLY);
        }
    }

    @Test
    void tryMIDStringNumberNumber() throws Exception {
        String queryPattern = "SELECT _id, strVal, '%s' as `number`, (SUBSTRING(strVal FROM (%d - 1) FOR CHAR_LENGTH(strVal))) as `mid` FROM test_string_collection";
        int[] tests = new int[] {1,2,3,11,12,20};

        for (int elem : tests) {
            String query = String.format(queryPattern, elem, elem);
            System.out.println("Query " + query);
            runExecuteQuery(query, VALUES_ONLY);
        }
    }

    @Test
    void tryMIDStringNumberZero() throws Exception {
        String queryPattern = "SELECT _id, strVal, '%s' as `number`, (SUBSTRING(strVal FROM (%d - 1) FOR 0)) as `mid` FROM test_string_collection";
        int[] tests = new int[] {1,2,3,11,12,20};

        for (int elem : tests) {
            String query = String.format(queryPattern, elem, elem);
            System.out.println("Query " + query);
            runExecuteQuery(query, VALUES_ONLY);
        }
    }

    @Test
    void trySTARTSWITHStringSubString() throws Exception {

        String query = "SELECT _id, strVal, (POSITION('toto' IN (TRIM(LEADING FROM strVal))) = 0) as `startswith` FROM test_string_collection";

        runExecuteQuery(query, VALUES_ONLY);

    }


    @Test
    void trySTDDEV() throws Exception {
        String query = "select STDDEV_POP(studentid) as `STDDEV_POP`, CAST(STDDEV_POP(studentid) * STDDEV_POP(studentid) AS DECIMAL) as `VAR_POP` from grades";
        //String query = "select studentid, STDDEV_POP(score) as `STDDEV_POP`, CAST(STDDEV_POP(score) * STDDEV_POP(score) AS DECIMAL) as `VAR_POP` from grades group by studentid";
        runExecuteQuery(query, METADATA_ONLY);
        runExecuteQuery(query, VALUES_ONLY);
        Statement stmt = m_conn.createStatement();
        ResultSet rs = stmt.executeQuery(query);
        rs.next();
        double stddev = rs.getDouble(1);
        BigDecimal bd = new BigDecimal(stddev);
        double varp = stddev * stddev;
        BigDecimal varpBd = bd.multiply(bd);
        System.out.println("stddev = " + stddev + ", varp = " + varp + ", varp bigDecimal = " + varpBd);
    }

    @Test
    void tryExecutePrintMetadata() throws Exception {
        runExecuteQuery("select * from Calcs", METADATA_ONLY);
    }

    @Test
    void tryGetCatalogs() throws Exception {
        ResultSet rs = m_dbMeta.getCatalogs();
        printRsInfo(rs);
    }

    @Test
    void tryGetTables() throws Exception {
        //ResultSet rs = m_dbMeta.getTables(null, null, null, null);
        ResultSet rs = m_dbMeta.getTables(null, null, "%", new String[]{"table", "view"});
        printRsInfo(rs);
    }

    @Test
    void tryGetColumns() throws Exception {
        ResultSet rs = m_dbMeta.getColumns(null, null, "Transactions", null);
        //ResultSet rs = m_dbMeta.getColumns(null, "%", "%", "%");
        printRsInfo(rs);
    }

    @Test
    void tryGetSchemas() throws Exception {
        ResultSet rs = m_dbMeta.getSchemas();
        //ResultSet rs = m_dbMeta.getSchemas(null, "filter");
        printRsInfo(rs);
    }

    @Test
    void tryGetTablePrivileges() throws Exception {
        //ResultSet rs = m_dbMeta.getTablePrivileges(null, null, null);
        ResultSet rs = m_dbMeta.getTablePrivileges(null, "%", "%");
        printRsInfo(rs);
    }

    @Test
    void tryGetTableTypes() throws Exception {
        ResultSet rs = m_dbMeta.getTableTypes();
        printRsInfo(rs);
    }

    @Test
    void tryVersionColumns() throws Exception {
        //ResultSet rs = m_dbMeta.getVersionColumns(null, null, null);
        ResultSet rs = m_dbMeta.getVersionColumns(null, "%", "%");
        printRsInfo(rs);
    }

    @Test
    void tryGetFunctions() throws Exception {
        ResultSet rs = m_dbMeta.getFunctions(null, null, null);
        //ResultSet rs = m_dbMeta.getFunctions(null, "%", "%");

        printRsInfo(rs);
    }

    @Test
    void tryGetFunctionColumns() throws Exception {
        //ResultSet rs = m_dbMeta.getFunctionColumns(null, null, null, null);
        ResultSet rs = m_dbMeta.getFunctionColumns(null, "%", "%", "%");
        printRsInfo(rs);
    }

    @Test
    void tryGetProcedures() throws Exception {
        //ResultSet rs = m_dbMeta.getProcedures(null,null,null);
        ResultSet rs = m_dbMeta.getProcedures(null,"%","%");
        printRsInfo(rs);
    }

    @Test
    void tryGetProcedureColumns() throws Exception {
        //ResultSet rs = m_dbMeta.getProcedureColumns(null,null,null, null);
        ResultSet rs = m_dbMeta.getProcedureColumns(null,"%","%", "%");
        printRsInfo(rs);
    }

    @Test
    void tryGetClientInfoProperties() throws Exception {
        ResultSet rs = m_dbMeta.getClientInfoProperties();
        printRsInfo(rs);
    }

    @Test
    void tryGetAttributes() throws Exception {
        //ResultSet rs = m_dbMeta.getAttributes(null, null,null,null);
        ResultSet rs = m_dbMeta.getAttributes(null, "%","%","%");
        printRsInfo(rs);
    }

    @Test
    void tryGetBestRowIdentifier() throws Exception {
        //ResultSet rs = m_dbMeta.getBestRowIdentifier(null,null,null,0,false);
        ResultSet rs = m_dbMeta.getBestRowIdentifier(null,null,"myTable",0,false);
        printRsInfo(rs);
    }

    @Test
    void tryGetColumnPrivileges() throws Exception {
        //ResultSet rs = m_dbMeta.getColumnPrivileges(null,null,null,null);
        ResultSet rs = m_dbMeta.getColumnPrivileges(null,null,"myTable","%");
        printRsInfo(rs);
    }

    @Test
    void tryGetCrossReference() throws Exception {
        //ResultSet rs = m_dbMeta.getCrossReference(null,null,null,null,null, null);
        ResultSet rs = m_dbMeta.getCrossReference(null,null,"parentTable","foreignCatalog",null,"foreignTable");
        printRsInfo(rs);
    }

    @Test
    void tryGetExportedKeys() throws Exception {
        //ResultSet rs = m_dbMeta.getExportedKeys(null,null,null);
        ResultSet rs = m_dbMeta.getExportedKeys(null,null,"myTable");
        printRsInfo(rs);
    }

    @Test
    void getImportedKeys() throws Exception {
        //ResultSet rs = m_dbMeta.getImportedKeys(null,null,null);
        ResultSet rs = m_dbMeta.getImportedKeys(null,null,"myTable");
        printRsInfo(rs);
    }

    @Test
    void tryGetIndexInfo() throws Exception {
        ResultSet rs = m_dbMeta.getIndexInfo(null,null,null,false, false);
        printRsInfo(rs);
    }

    @Test
    void tryGetPrimaryKeys() throws Exception {
        //ResultSet rs = m_dbMeta.getPrimaryKeys(null,null,null);
        ResultSet rs = m_dbMeta.getPrimaryKeys(null,null,"myTable");
        printRsInfo(rs);
    }

    @Test
    void tryGetPseudoColumns() throws Exception {
        //ResultSet rs = m_dbMeta.getPseudoColumns(null,null,null,null);
        ResultSet rs = m_dbMeta.getPseudoColumns(null,"%","%","%");
        printRsInfo(rs);
    }

    @Test
    void tryGetSuperTables() throws Exception {
        ResultSet rs = m_dbMeta.getSuperTables(null,null,null);
        printRsInfo(rs);
    }

    @Test
    void tryGetSuperTypes() throws Exception {
        ResultSet rs = m_dbMeta.getSuperTypes(null,null,null);
        printRsInfo(rs);
    }

    @Test
    void tryGetTypeInfo() throws Exception {
        ResultSet rs = m_dbMeta.getTypeInfo();
        printRsInfo(rs);
    }

    @Test
    void tryGetUDTs() throws Exception {
        ResultSet rs = m_dbMeta.getUDTs(null,null,null, new int[]{1});
        printRsInfo(rs);
    }

    @Test
    void tryDBMeta() throws Exception {
        System.out
                .println("-----------------------------------------------------------------------");
        System.out
                .println("-----------------------------------------------------------------------");
        System.out.println("API databases metadata");
        System.out.println("Driver Name: " + m_dbMeta.getDriverName());
        System.out.println("Driver Version: " + m_dbMeta.getDriverVersion());
        System.out.println("Driver Major Version: "
                + m_dbMeta.getDriverMajorVersion());
        System.out.println("Driver Minor Version: "
                + m_dbMeta.getDriverMinorVersion());
        System.out.println("Database Product Name: "
                + m_dbMeta.getDatabaseProductName());
        System.out.println("Database Product version: "
                + m_dbMeta.getDatabaseProductVersion());
        System.out.println("Database Major version: "
                + m_dbMeta.getDatabaseMajorVersion());
        System.out.println("Database Minor version : "
                + m_dbMeta.getDatabaseMinorVersion());
        System.out.println("Supports Mixed Case Quoted Identifiers: "
                + m_dbMeta.supportsMixedCaseQuotedIdentifiers());
        System.out.println("Supports Mixed Case Identifiers: "
                + m_dbMeta.supportsMixedCaseIdentifiers());
        System.out.println("Stores UpperCase Quoted Identifiers: "
                + m_dbMeta.storesUpperCaseQuotedIdentifiers());
        System.out.println("Stores LowerCase Quoted Identifiers: "
                + m_dbMeta.storesLowerCaseQuotedIdentifiers());
        System.out.println("Stores Mixed Case Quoted Identifiers: "
                + m_dbMeta.storesMixedCaseQuotedIdentifiers());
        System.out.println("Stores Upper Case Identifiers: "
                + m_dbMeta.storesUpperCaseIdentifiers());
        System.out.println("Stores Lower Case Identifiers: "
                + m_dbMeta.storesLowerCaseIdentifiers());
        System.out.println("Stores Mixed Case Identifiers: "
                + m_dbMeta.storesMixedCaseIdentifiers());
        System.out.println("Identifier Quote String: "
                + m_dbMeta.getIdentifierQuoteString());
        System.out.println("Is ReadOnly: " + m_dbMeta.isReadOnly());
        System.out.println("Supports ResultSet Concurrency: "
                + m_dbMeta.supportsResultSetConcurrency(ResultSet.TYPE_FORWARD_ONLY,
                ResultSet.FETCH_UNKNOWN));

        System.out.println("get Identifier Quote String: "
                + m_dbMeta.getJDBCMajorVersion());
        System.out.println("get Identifier Quote String: "
                + m_dbMeta.getTimeDateFunctions());
        System.out.println("get Identifier Quote String: "
                + m_dbMeta.getStringFunctions());
        System.out.println("get Identifier Quote String: "
                + m_dbMeta.getNumericFunctions());
    }

    @Test
    void tryGetUrl() throws Exception {
        System.out.println(m_dbMeta.getURL());
    }

    @Test
    void trySetCatalog() throws SQLException {
        m_conn.setCatalog("test_set_catalog");
        System.out.println(m_conn.getCatalog());
    }

    //@Test
    // TODO : Cancel is not supported yet
    void tryCancel() throws SQLException {
        // The query must be a long running query for cancel to be effective
        String query = "select * from class";
        System.out.println("-----------------------------------------------------------------------");

        class QueryExecutor implements Runnable
        {
            private boolean m_isExecuting;
            private final String m_query;
            private ResultSet m_result;
            private final Statement m_statement;

            public QueryExecutor(Statement statement, String query) {
                m_statement = statement;
                m_query = query;
                m_isExecuting = false;
            }

            public boolean getIsExecuting() {
                return m_isExecuting;
            }

            public ResultSet getResultSet() {
                return m_result;
            }

            @Override
            public void run()
            {
                try {
                    System.out.println("Executing '" + m_query + "'");
                    m_isExecuting = true;
                    m_result = m_statement.executeQuery(m_query);
                    m_isExecuting = false;
                }
                catch (SQLException e) {
                    e.printStackTrace();
                }
                finally {
                    try {
                        m_result.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        Statement statement = m_conn.createStatement();
        ResultSet result = null;

        QueryExecutor queryExecutor = new QueryExecutor(statement, query);
        Thread execThread = new Thread(queryExecutor);
        execThread.start();

        while (!queryExecutor.getIsExecuting()) {
            ;
        }
        if (execThread.isAlive()) {
            try {
                // Still need to wait because the m_isExecuting flag is set  before statement.executeQuery is called.
                // Need to wait until an executor object is created.
                Thread.sleep(2000);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("Calling cancel");
            statement.cancel();
            System.out.println("The query cancelled successfully.");

            // Try to use the statement again
            queryExecutor = new QueryExecutor(statement, query);
            execThread = new Thread(queryExecutor);
            execThread.start();
        }
        else {
            System.out.println("The execution finished before cancel");
            queryExecutor.getResultSet().close();
        }

        System.out.println("-----------------------------------------------------------------------");
    }

    /**
     * Execute the given query and print either the resultset contents or its metadata.
     * @param query             The query to execute.
     * @param rsPrintTypes      A list of things to print out. It can be : the values, the metadata or the json schema.
     * @throws Exception        If an error occurs while executing the query or retrieving the values to display.
     */
    void runExecuteQuery(String query, Set<RsPrintType> rsPrintTypes) throws Exception {
        try {
            Statement stmt = m_conn.createStatement();
            long startTime = System.nanoTime();

            ResultSet rs = stmt.executeQuery(query);
            long endTime = System.nanoTime();
            long timeElapsed = endTime - startTime;
            System.out.println("Execution time in milliseconds: " + timeElapsed / 1000000);
            if (rs.getMetaData().getColumnCount() > 0) {
                rs.getMetaData().isCaseSensitive(1);
            }
            if (null == rsPrintTypes || rsPrintTypes.contains(RsPrintType.METADATA)) {
                PrintUtils.printResultSetMetadata(rs.getMetaData());
            }

            if (null == rsPrintTypes ||rsPrintTypes.contains(RsPrintType.VALUES)) {
                PrintUtils.printResultSet(rs);
            }

            if (null == rsPrintTypes ||rsPrintTypes.contains(RsPrintType.JSON_SCHEMA)) {
                MongoResultSet mrs = rs.unwrap(MongoResultSet.class);
               System.out.println(mrs.getJsonSchema());
            }

        }
        catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Prints the resultset metadata followed by its contents.
     * @param rs            The result set to display information for.
     * @throws Exception    If an error occurs when retrieving the information to display.
     */
    private void printRsInfo(ResultSet rs) throws Exception {
        try {
            PrintUtils.printResultSetMetadata(rs.getMetaData());
            PrintUtils.printResultSet(rs);
        }
        catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }
}
