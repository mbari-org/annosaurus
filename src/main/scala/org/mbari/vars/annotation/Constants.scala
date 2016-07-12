package org.mbari.vars.annotation

import java.lang.reflect.Type
import java.time.Duration

import com.fatboyindustrial.gsonjavatime.Converters
import com.google.gson.reflect.TypeToken
import com.google.gson.{ FieldNamingPolicy, GsonBuilder }
import org.mbari.vars.annotation.json.{ DurationConverter, OptionConverter, TimecodeConverter }
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
  val GSON = {

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

    gsonBuilder.create()

  }

  val GSON_FOR_ANNOTATION = {
    val gsonBuilder = new GsonBuilder()
      .setPrettyPrinting()
      .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
      .setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
    Converters.registerInstant(gsonBuilder)

    val durationType: Type = new TypeToken[Duration]() {}.getType
    gsonBuilder.registerTypeAdapter(durationType, new DurationConverter)

    val timecodeType: Type = new TypeToken[Timecode]() {}.getType
    gsonBuilder.registerTypeAdapter(timecodeType, new TimecodeConverter)

    val optionType: Type = new TypeToken[Option[_]]() {}.getType
    gsonBuilder.registerTypeAdapter(optionType, new OptionConverter)

    gsonBuilder.create()
  }

}
