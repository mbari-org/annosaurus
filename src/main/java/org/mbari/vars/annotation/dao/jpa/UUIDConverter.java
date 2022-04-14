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

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.util.UUID;

/**
 * Created by brian on 5/12/16.
 * @author Brian Schlining
 * @since 2016-05-12
 */
@Converter(autoApply = true)
public class UUIDConverter implements AttributeConverter<UUID, Object> {

    private static final String databaseProductName = DatabaseProductName.name();

    @Override
    public Object convertToDatabaseColumn(UUID uuid) {
        if (uuid == null) {
          return null;
        }
        else if (databaseProductName.equalsIgnoreCase("postgresql")) {
          return uuid;
        }
        return uuid.toString().toLowerCase();
    }

    @Override
    public UUID convertToEntityAttribute(Object obj) {
        if (obj == null) {
            return null;
        }
        else if (obj instanceof UUID uuid) {
            return uuid;
        }
        return UUID.fromString(obj.toString().toLowerCase());
    }
}
