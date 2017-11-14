package org.mbari.vars.annotation.gson;

import com.google.gson.InstanceCreator;
import org.mbari.vars.annotation.dao.jpa.AssociationImpl;
import org.mbari.vars.annotation.model.Association;

import java.lang.reflect.Type;

/**
 * @author Brian Schlining
 * @since 2017-09-20T14:15:00
 */
public class AssociationCreator implements InstanceCreator<Association> {
    @Override
    public Association createInstance(Type type) {
        return new AssociationImpl();
    }
}
