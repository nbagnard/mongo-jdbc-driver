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

package com.mongodb.jdbc;

import static com.mongodb.jdbc.mongosql.Namespaces.NAMESPACES_CODEC;
import static com.mongodb.jdbc.mongosql.TranslationResult.TRANSLATION_RESULT_CODEC_CODEC;

import com.mongodb.jdbc.mongosql.Namespace;
import com.mongodb.jdbc.mongosql.Namespaces;
import com.mongodb.jdbc.mongosql.TranslationHelpers;
import com.mongodb.jdbc.mongosql.TranslationResult;
import java.nio.ByteBuffer;
import java.util.Base64;
import org.bson.*;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.DocumentCodec;
import org.bson.codecs.EncoderContext;
import org.bson.io.BasicOutputBuffer;
import org.junit.jupiter.api.Test;

public class TranslationHelpersTest {

    @Test
    public void testGetNamespace() throws Exception {
        TranslationHelpers.loadLib();
        String namespacesBase64 =
                TranslationHelpers.getNamespaces("integration_test", "select * from foo");
        byte[] rawContent = Base64.getDecoder().decode(namespacesBase64);
        Namespaces namespaces = new RawBsonDocument(rawContent).decode(NAMESPACES_CODEC);

        for (Namespace namespace : namespaces.namespaces) {
            System.out.println(
                    "Database = " + namespace.database + ", Collection = " + namespace.collection);
        }
    }

    @Test
    public void testTranslate() throws Exception {
        TranslationHelpers.loadLib();
        String catalogStr =
                "{\"integration_test\": {\"foo\": {\"bsonType\": [\"object\"], \"properties\": {\"_id\": {\"bsonType\": [\"int\"], \"additionalProperties\": false}, \"a\": {\"bsonType\": [\"long\"], \"additionalProperties\": false}}, \"additionalProperties\": false}}}\0";
        Document catalogDoc = Document.parse(catalogStr);
        final byte[] catalog = toBytes(catalogDoc);
        System.out.println(new RawBsonDocument(catalog).toJson());
        String base64Catalog = Base64.getEncoder().encodeToString(catalog);

        System.out.println(new RawBsonDocument(Base64.getDecoder().decode(base64Catalog)).toJson());

        String currentDB = "integration_test";
        String sql = "select * from foo";
        int relaxSchemaChecking = 1;
        System.out.println(
                "Calling mongosql-rs.translate with  "
                        + currentDB
                        + ","
                        + sql
                        + ","
                        + new RawBsonDocument(Base64.getDecoder().decode(base64Catalog)).toJson()
                        + ","
                        + relaxSchemaChecking);
        String translationResultBase64 =
                TranslationHelpers.translate(currentDB, sql, base64Catalog, relaxSchemaChecking);
        byte[] rawContent = Base64.getDecoder().decode(translationResultBase64);
        System.out.println(new RawBsonDocument(rawContent).toJson());

        TranslationResult result =
                new RawBsonDocument(rawContent).decode(TRANSLATION_RESULT_CODEC_CODEC);
        System.out.println("targetDb = " + result.targetDb);
        System.out.println("targetCollection = " + result.targetCollection);
        System.out.println("pipeline = " + result.pipeline);
        System.out.println("resulSetSchema = " + result.resulSetSchema);
    }

    private static final Codec<Document> DOCUMENT_CODEC = new DocumentCodec();

    public static byte[] toBytes(Document document) {
        try (BasicOutputBuffer buffer = new BasicOutputBuffer()) {
            BsonBinaryWriter writer = new BsonBinaryWriter(buffer);
            DOCUMENT_CODEC.encode(
                    writer,
                    document,
                    EncoderContext.builder().isEncodingCollectibleDocument(true).build());
            return buffer.toByteArray();
        }
    }

    public static Document toDocument(byte[] bytes) {
        try (BsonBinaryReader reader = new BsonBinaryReader(ByteBuffer.wrap(bytes))) {
            Document document = DOCUMENT_CODEC.decode(reader, DecoderContext.builder().build());
            return document;
        }
    }
}
