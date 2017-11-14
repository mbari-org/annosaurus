package org.mbari.vars.annotation.gson

import java.lang.reflect.Type
import java.lang.{Float => JFloat, Double => JDouble}

import com.google.gson._

/**
  * @author Brian Schlining
  * @since 2017-11-14T11:42:00
  */
abstract class OptionSerializer extends JsonSerializer[Option[Any]] with JsonDeserializer[Option[Any]]{
  override def serialize(src: Option[Any], typeOfSrc: Type, context: JsonSerializationContext): JsonElement = {
    src match {
      case None => JsonNull.INSTANCE
      case Some(i: Int) => context.serialize(i)
      case Some(l: Long) => context.serialize(l)
      case Some(d: Double) => context.serialize(d)
      case Some(f: Float) => context.serialize(f)
      case v => context.serialize(v)
    }
  }

  override def deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext) = {
    val t = classOf[Number]
    Option(context.deserialize(json, t))
  }
}

class FloatOptionDeserializer extends OptionSerializer {
  override def deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext) = {
    val t = classOf[JFloat]
    Option(context.deserialize(json, t))
  }
}

class DoubleOptionDeserializer extends OptionSerializer {
  override def deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext) = {
    val t = classOf[JDouble]
    Option(context.deserialize(json, t))
  }
}
