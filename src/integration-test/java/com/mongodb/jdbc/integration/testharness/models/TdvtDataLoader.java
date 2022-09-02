/*
 * Copyright 2022-present MongoDB, Inc.
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

package com.mongodb.jdbc.integration.testharness.models;

import com.mongodb.jdbc.integration.testharness.DataLoader;
import java.io.IOException;

public class TdvtDataLoader {
    public static final String TEST_DATA_DIRECTORY = "resources/tdvt_test/testdata";

    public static final String TDVT_MDB_URL =
            "mongodb+srv://m001-student:m001-mongodb-basics@sandbox.1aac4.mongodb.net";
    public static final String TDVT_ADL_URL =
            "mongodb://m001-student:m001-mongodb-basics@tdvtlake-1aac4.a.query.mongodb.net";

    public static void main(String[] args) throws IOException {
        DataLoader loader = new DataLoader(TEST_DATA_DIRECTORY, TDVT_MDB_URL, TDVT_ADL_URL);
        loader.dropCollections();
        loader.loadTestData();
    }
}
