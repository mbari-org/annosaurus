package org.mbari.vars.annotation.dao.jpa;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.util.UUID;

/**
 * Created by brian on 5/12/16.
 */
@Converter(autoApply = true)
public class UUIDConverter implements AttributeConverter<UUID, String> {

    @Override
    public String convertToDatabaseColumn(UUID uuid) {
        return uuidToString(uuid);
    }

    @Override
    public UUID convertToEntityAttribute(String s) {
        return stringToUUID(s);
    }

    public static String uuidToString(UUID uuid) {
        return uuid == null ? null : uuid.toString().toUpperCase();
    }

    public static UUID stringToUUID(String s) {
        return s == null ? null : UUID.fromString(s.toUpperCase());
    }
}
