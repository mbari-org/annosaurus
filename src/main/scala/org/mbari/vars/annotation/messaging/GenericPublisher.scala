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

package org.mbari.vars.annotation.messaging

import io.reactivex.subjects.Subject
import org.mbari.vars.annotation.dao.jpa.AnnotationImpl
import org.mbari.vars.annotation.model.{Annotation, Association, Observation}

import scala.util.Try

/**
 * @author Brian Schlining
 * @since 2020-03-04T13:32:00
 */


trait GenericPublisher[A] {
  def publish(x: A)
  def publish(xs: Iterable[A]): Unit = for {
    x <- xs
  } publish(x)
  def publish(opt: Option[A]): Unit = opt match {
    case None => // do nothing
    case Some(a) => Try(publish(a))
  }
}
/**
 * Decorator for an reactive Subject that publishes AnnotationMessages for
 * common use cases.
 * @param subject
 */
class AnnotationPublisher(subject: Subject[Any]) extends GenericPublisher[Annotation] {
  def publish(annotation: Annotation): Unit = Try(subject.onNext(AnnotationMessage(annotation)))
  def publish(observation: Observation): Unit = publish(AnnotationImpl(observation))
}

class AssociationPublisher(subject: Subject[Any]) extends GenericPublisher[Association] {
  def publish(association: Association): Unit = Try(subject.onNext(AssociationMessage(association)))
}
