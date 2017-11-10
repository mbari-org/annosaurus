package org.mbari.vars.annotation.dao.jpa;

import scala.Option;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/**
 * @author Brian Schlining
 * @since 2017-11-10T07:38:00
 */
@Converter(autoApply = true)
public class OptionConverter<A> implements AttributeConverter<scala.Option<A>, A> {

    @Override
    public A convertToDatabaseColumn(Option<A> opt) {
        return opt == null || opt.isEmpty() ? null : opt.get();
    }

    @Override
    public Option<A> convertToEntityAttribute(A a) {
        return scala.Option.apply(a);
    }
}


