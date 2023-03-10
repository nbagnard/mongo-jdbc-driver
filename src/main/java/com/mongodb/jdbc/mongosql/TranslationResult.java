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

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;

import com.mongodb.jdbc.JsonSchema;
import com.mongodb.jdbc.MongoJsonSchema;
import java.util.List;
import org.bson.BsonDocument;
import org.bson.codecs.BsonValueCodecProvider;
import org.bson.codecs.Codec;
import org.bson.codecs.ValueCodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.codecs.pojo.annotations.BsonCreator;
import org.bson.codecs.pojo.annotations.BsonProperty;

public class TranslationResult {
    private static final CodecRegistry REGISTRY =
            fromProviders(
                    new BsonValueCodecProvider(),
                    new ValueCodecProvider(),
                    PojoCodecProvider.builder().automatic(true).build());

    public static final Codec<TranslationResult> TRANSLATION_RESULT_CODEC_CODEC =
            REGISTRY.get(TranslationResult.class);

    public String targetDb;
    public String targetCollection;
    public List<BsonDocument> pipeline;
    public MongoJsonSchema resulSetSchema;

    @BsonCreator
    public TranslationResult(
            @BsonProperty("target_db") final String targetDb,
            @BsonProperty("target_collection") final String targetCollection,
            @BsonProperty("pipeline") final List<BsonDocument> pipeline,
            @BsonProperty("result_set_schema") JsonSchema resulSetSchema) {
        this.targetDb = targetDb;
        this.targetCollection = targetCollection;
        this.pipeline = pipeline;
        this.resulSetSchema = MongoJsonSchema.toSimplifiedMongoJsonSchema(resulSetSchema);
    }
}
