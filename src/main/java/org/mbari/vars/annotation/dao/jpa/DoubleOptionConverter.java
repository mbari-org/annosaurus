package org.mbari.vars.annotation.dao.jpa;

import scala.Option;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/**
 * @author Brian Schlining
 * @since 2017-11-10T07:55:00
 */
@Converter(autoApply = true)
public class DoubleOptionConverter implements AttributeConverter<Option<Double>, Double> {

    @Override
    public Double convertToDatabaseColumn(Option<Double> opt) {
        return opt == null || opt.isEmpty() ? null : opt.get();
    }

    @Override
    public Option<Double> convertToEntityAttribute(Double a) {
        return scala.Option.apply(a);
    }
}