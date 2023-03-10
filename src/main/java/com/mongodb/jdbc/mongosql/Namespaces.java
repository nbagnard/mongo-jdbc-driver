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

import java.util.Set;
import org.bson.codecs.BsonValueCodecProvider;
import org.bson.codecs.Codec;
import org.bson.codecs.ValueCodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

public class Namespaces {
    private static final CodecRegistry REGISTRY =
            fromProviders(
                    new BsonValueCodecProvider(),
                    new ValueCodecProvider(),
                    PojoCodecProvider.builder().automatic(true).build());

    public static final Codec<Namespaces> NAMESPACES_CODEC = REGISTRY.get(Namespaces.class);

    public Set<Namespace> namespaces;
}
