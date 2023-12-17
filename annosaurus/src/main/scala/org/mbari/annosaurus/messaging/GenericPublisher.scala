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

import io.reactivex.rxjava3.subjects.Subject
import org.mbari.annosaurus.model.MutableObservation
import org.mbari.annosaurus.model.{MutableAnnotation, MutableAssociation}
import org.mbari.annosaurus.repository.jpa.MutableAnnotationImpl

import scala.util.Try

/** @author
  *   Brian Schlining
  * @since 2020-03-04T13:32:00
  */
trait GenericPublisher[A] {
    def publish(x: A): Unit
    def publish(xs: Iterable[A]): Unit =
        for {
            x <- xs
        } publish(x)
    def publish(opt: Option[A]): Unit  = opt match {
        case None    => // do nothing
        case Some(a) => Try(publish(a))
    }
}

/** Decorator for an reactive Subject that publishes AnnotationMessages for common use cases.
  * @param subject
  */
class AnnotationPublisher(subject: Subject[Any]) extends GenericPublisher[MutableAnnotation] {
    def publish(annotation: MutableAnnotation): Unit   = Try(
        subject.onNext(AnnotationMessage(annotation))
    )
    def publish(observation: MutableObservation): Unit = publish(MutableAnnotationImpl(observation))
}

class AssociationPublisher(subject: Subject[Any]) extends GenericPublisher[MutableAssociation] {
    def publish(association: MutableAssociation): Unit = Try(
        subject.onNext(AssociationMessage(association))
    )
}
