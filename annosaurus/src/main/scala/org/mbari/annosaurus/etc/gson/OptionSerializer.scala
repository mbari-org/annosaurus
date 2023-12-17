/*
 * Copyright 2017 Monterey Bay Aquarium Research Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mbari.annosaurus.etc.gson

import com.google.gson._

import java.lang.reflect.{ParameterizedType, Type}

/** @author
  *   Brian Schlining
  * @since 2017-11-14T11:42:00
  */
class OptionSerializer extends JsonSerializer[Option[Any]] with JsonDeserializer[Option[Any]] {

    private def innerType(outerType: Type) =
        outerType match {
            case pt: ParameterizedType => pt.getActualTypeArguments()(0)
            case _                     => throw new UnsupportedOperationException("Expected ParameterizedType")
        }

    override def serialize(
        src: Option[Any],
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement = {
        src match {
            case None    => JsonNull.INSTANCE
            case Some(v) => context.serialize(v, innerType(typeOfSrc))
        }
    }
    override def deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): Option[Any] = {
        json match {
            case null                 => None
            case _ if json.isJsonNull => None
            case _                    => Some(context.deserialize(json, innerType(typeOfT)))
        }
    }
}
