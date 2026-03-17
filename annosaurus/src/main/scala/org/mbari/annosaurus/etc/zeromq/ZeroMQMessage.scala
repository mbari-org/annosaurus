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

package org.mbari.annosaurus.etc.zeromq

import org.mbari.annosaurus.domain.{Annotation, Association}
import org.mbari.annosaurus.messaging.Message
import org.mbari.annosaurus.etc.circe.CirceCodecs.{*, given}


trait ZeroMQMessage[+T] extends Message[T]
/**
 * Send when a new annotation is created or an existing one is updated
 *
 * @param content
 */
case class AnnotationCreatedMessage(content: Annotation) extends ZeroMQMessage[Annotation]:

    override def hashCode(): Int =
        this.content.observationUuid.hashCode() +
            this.content.observationTimestamp.hashCode() * 3

    override def equals(obj: Any): Boolean =
        obj match
            case that: AnnotationCreatedMessage =>
                this.content.observationUuid == that.content.observationUuid &&
                    this.content.observationTimestamp == that.content.observationTimestamp
            case _                       => false

    override def toJson: String = content.stringify

case class AssociationCreatedMessage(content: Association) extends Message[Association]:
    //  override def hashCode(): Int = this.content.uuid.hashCode()
    //
    //  override def equals(obj: Any): Boolean = obj match {
    //    case that: AssociationMessage => this.content.uuid == that.content.uuid
    //    case _ => false
    //  }

    override def toJson: String =
        content.stringify

