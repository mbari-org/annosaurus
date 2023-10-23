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

package org.mbari.vars.annotation.gson;

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
