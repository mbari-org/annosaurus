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
