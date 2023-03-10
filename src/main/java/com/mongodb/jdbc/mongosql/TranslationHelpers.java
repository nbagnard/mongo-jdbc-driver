/*
 * Copyright 2023-present MongoDB, Inc.
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

package com.mongodb.jdbc.mongosql;

public class TranslationHelpers {

    /**
     * Native mongosqlj method. Returns the namespaces associated with the provided current DB and
     * Sql statement.
     *
     * @param currentDB
     * @param sql
     * @return a base-64 encoded BSON document with the following format {"namespaces" : [
     *     {"database": "XXX", "collection": "XXXX"}, {"database": "XXX", "collection": "XXXX"}, ...
     *     ] }
     */
    public static native String getNamespaces(String currentDB, String sql);

    /**
     * Native mongosqlj method. Returns a base64-encoded bson representation of Translation for the
     * provided SQL query, database, catalog schema, and schema checking mode. { "target_db": "XXXX"
     * "target_collection": "XXXX", "pipeline": "XXXX", "result_set_schema": "XXXX", }
     */
    public static native String translate(
            String currentDB, String sql, String catalog, int relaxSchemaChecking);

    private static boolean isLibLoaded = false;

    public static void loadLib() {
        if (!isLibLoaded) {
            System.loadLibrary("mongosqlj");
        }
        isLibLoaded = true;
    }
}
