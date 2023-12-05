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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URI;

/**
 * @author Brian Schlining
 * @since 2016-06-17T13:17:00
 */
@Converter(autoApply = true)
public class URLConverter implements AttributeConverter<URL, String> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public String convertToDatabaseColumn(URL url) {
        return url == null ? null : url.toExternalForm();
    }

    @Override
    public URL convertToEntityAttribute(String s) {
        URL url = null;
        if (s != null) {
            try {
                // url = URI.create(s).toURL(); // This causes tests to fail
                // var t = java.net.URLEncoder.encode(s, "UTF-8");
                // url = URI.create(t).toURL();
                url = new URL(s);
            }
            catch (MalformedURLException e) {
                log.warn("Bad URL found. Could not convert " + s + " to a URL");
            }
        }
        return url;
    }
}
