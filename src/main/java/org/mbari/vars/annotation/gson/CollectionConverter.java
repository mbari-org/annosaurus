package org.mbari.vars.annotation.gson;



import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;

/**
 * @author Brian Schlining
 * @since 2019-02-08T10:53:00
 */
public class CollectionConverter implements JsonSerializer<Collection<?>> {
    @Override
    public JsonElement serialize(Collection<?> src, Type typeOfSrc, JsonSerializationContext context) {
        if (src == null || src.isEmpty()) {
            return null;
        }

        JsonArray array = new JsonArray();
        for (Object child : src) {
            JsonElement element = context.serialize(child);
            array.add(element);
        }
        return array;
    }
}
