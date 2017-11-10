package org.mbari.vars.annotation.dao.jpa;

import scala.Option;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/**
 * @author Brian Schlining
 * @since 2017-11-10T07:53:00
 */
@Converter(autoApply = true)
public class FloatOptionConverter implements AttributeConverter<Option<Float>, Float> {

    @Override
    public Float convertToDatabaseColumn(Option<Float> opt) {
        return opt == null || opt.isEmpty() ? null : opt.get();
    }

    @Override
    public Option<Float> convertToEntityAttribute(Float a) {
        return scala.Option.apply(a);
    }
}