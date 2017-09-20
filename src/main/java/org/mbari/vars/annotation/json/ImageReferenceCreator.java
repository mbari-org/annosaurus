package org.mbari.vars.annotation.json;

import com.google.gson.InstanceCreator;
import org.mbari.vars.annotation.dao.jpa.ImageReferenceImpl;
import org.mbari.vars.annotation.model.ImageReference;

import java.lang.reflect.Type;

/**
 * @author Brian Schlining
 * @since 2017-09-20T14:14:00
 */
public class ImageReferenceCreator implements InstanceCreator<ImageReference> {
    @Override
    public ImageReference createInstance(Type type) {
        return new ImageReferenceImpl();
    }
}
