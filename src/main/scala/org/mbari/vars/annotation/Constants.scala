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

package org.mbari.vars.annotation

import java.lang.reflect.Type
import java.time.Duration
import java.util

import com.fatboyindustrial.gsonjavatime.Converters
import com.google.gson.reflect.TypeToken
import com.google.gson.{ FieldNamingPolicy, Gson, GsonBuilder }
import org.mbari.vars.annotation.gson._
import org.mbari.vars.annotation.model.{ Association, ImageReference }
import org.mbari.vcr4j.time.Timecode

/**
 *
 *
 * @author Brian Schlining
 * @since 2016-07-11T15:53:00
 */
object Constants {

  /**
   * Gson parser configured for the VAM's use cases.
   */
  val GSON: Gson = {

    val gsonBuilder = new GsonBuilder()
      .setPrettyPrinting()
      .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
      .excludeFieldsWithoutExposeAnnotation()
      .setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
    Converters.registerInstant(gsonBuilder)

    val durationType: Type = new TypeToken[Duration]() {}.getType
    gsonBuilder.registerTypeAdapter(durationType, new DurationConverter)

    val timecodeType: Type = new TypeToken[Timecode]() {}.getType
    gsonBuilder.registerTypeAdapter(timecodeType, new TimecodeConverter)

    val associationType: Type = new TypeToken[Association]() {}.getType
    gsonBuilder.registerTypeAdapter(associationType, new AssociationCreator)

    val imageReferenceType: Type = new TypeToken[ImageReference]() {}.getType
    gsonBuilder.registerTypeAdapter(imageReferenceType, new ImageReferenceCreator)

    gsonBuilder.registerTypeAdapter(classOf[Option[Any]], new OptionSerializer)

    gsonBuilder.registerTypeAdapter(classOf[util.Collection[_]], new CollectionConverter)

    gsonBuilder.create()

  }

  val GSON_FOR_ANNOTATION: Gson = GSON
  //  {
  //    val gsonBuilder = new GsonBuilder()
  //      .setPrettyPrinting()
  //      .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
  //      .setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
  //    Converters.registerInstant(gsonBuilder)
  //
  //    val durationType: Type = new TypeToken[Duration]() {}.getType
  //    gsonBuilder.registerTypeAdapter(durationType, new DurationConverter)
  //
  //    val timecodeType: Type = new TypeToken[Timecode]() {}.getType
  //    gsonBuilder.registerTypeAdapter(timecodeType, new TimecodeConverter)
  //
  //    val optionType: Type = new TypeToken[Option[_]]() {}.getType
  //    gsonBuilder.registerTypeAdapter(optionType, new OptionConverter)
  //
  //    val associationType: Type = new TypeToken[Association]() {}.getType
  //    gsonBuilder.registerTypeAdapter(associationType, new AssociationCreator)
  //
  //    val imageReferenceType: Type = new TypeToken[ImageReference]() {}.getType
  //    gsonBuilder.registerTypeAdapter(imageReferenceType, new ImageReferenceCreator)
  //
  //    gsonBuilder.create()
  //  }

}
