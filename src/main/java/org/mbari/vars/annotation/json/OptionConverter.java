package org.mbari.vars.annotation.json;


import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import scala.Option;

import java.lang.reflect.Type;

/**
 * @author Brian Schlining
 * @since 2016-07-12T12:09:00
 */
public class OptionConverter implements JsonSerializer<scala.Option<?>> {

    @Override
    public JsonElement serialize(Option<?> src, Type typeOfSrc, JsonSerializationContext context) {
        if (src.isDefined()) {
            return context.serialize(src.get());
        }
        else {
            return JsonNull.INSTANCE;
        }
    }

}
