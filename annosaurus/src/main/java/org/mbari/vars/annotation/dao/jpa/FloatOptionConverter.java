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

package org.mbari.vars.annotation.dao.jpa;

import scala.Option;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * @author Brian Schlining
 * @since 2017-11-10T07:53:00
 * @deprecated Causes issues with JPA. Eclipslink is reading Float columns as Doubles so conversion fails
 */
@Deprecated(since="2020-01-01T00:00:00Z")
@Converter(autoApply = true)
public class FloatOptionConverter implements AttributeConverter<Option<Float>, Float> {

    @Override
    public Float convertToDatabaseColumn(Option<Float> opt) {
        try {
            return opt == null || opt.isEmpty() ? null : opt.get().floatValue();
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    @Override
    public Option<Float> convertToEntityAttribute(Float a) {
        return scala.Option.apply(a.floatValue());
    }
}