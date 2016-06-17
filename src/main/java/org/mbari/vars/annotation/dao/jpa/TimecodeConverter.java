package org.mbari.vars.annotation.dao.jpa;

import org.mbari.vcr4j.time.Timecode;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/**
 * @author Brian Schlining
 * @since 2016-06-16T14:17:00
 */
@Converter(autoApply = true)
public class TimecodeConverter implements AttributeConverter<Timecode, String> {

    @Override
    public String convertToDatabaseColumn(Timecode timecode) {
        return timecode == null ? null : timecode.toString();
    }

    @Override
    public Timecode convertToEntityAttribute(String s) {
        return s == null ? null : new Timecode(s);
    }
}
