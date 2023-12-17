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

package org.mbari.annosaurus.messaging

import com.fatboyindustrial.gsonjavatime.Converters
import com.google.gson.{FieldNamingPolicy, GsonBuilder}
import org.mbari.annosaurus.Constants
import org.mbari.annosaurus.model.simple.ExtendedAssociation
import org.mbari.annosaurus.model.{MutableAnnotation, MutableAssociation}

/** @author
  *   Brian Schlining
  * @since 2020-03-04T13:53:00
  */
trait JsonEncoder[A] {
    def toJson: String
}

object JsonEncoders {

    private[this] val caseClassGson = {
        val builder = new GsonBuilder()
            .setPrettyPrinting()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
        Converters.registerInstant(builder)
        builder.create()
    }

    implicit class AnnotationEncoder(val x: MutableAnnotation)
        extends JsonEncoder[MutableAnnotation]   {
        override def toJson: String = Constants.GSON.toJson(x)
    }
    implicit class AssocationEncoder(val x: MutableAssociation)
        extends JsonEncoder[MutableAssociation]  {
        override def toJson: String = Constants.GSON.toJson(x)
    }
    implicit class ExtendedAssocationEncoder(val x: ExtendedAssociation)
        extends JsonEncoder[ExtendedAssociation] {
        override def toJson: String = caseClassGson.toJson(x)
    }
}
