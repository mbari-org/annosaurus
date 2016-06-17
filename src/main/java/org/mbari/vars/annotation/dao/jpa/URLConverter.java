package org.mbari.vars.annotation.dao.jpa;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.AttributeConverter;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author Brian Schlining
 * @since 2016-06-17T13:17:00
 */
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
                url = new URL(s);
            }
            catch (MalformedURLException e) {
                log.warn("Bad URL found. Could not convert " + s + " to a URL");
            }
        }
        return url;
    }
}
