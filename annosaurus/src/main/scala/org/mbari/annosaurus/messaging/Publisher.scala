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
import org.mbari.annosaurus.domain.{Annotation, Association, Observation}
import org.mbari.annosaurus.etc.zeromq.{AnnotationCreatedMessage, AssociationCreatedMessage, GenericPublisher}
import org.mbari.annosaurus.etc.jdk.Loggers.given
import org.mbari.annosaurus.etc.nats.NatsMessage

import scala.util.Try

/**
 * @author
 *   Brian Schlining
 * @since 2020-03-04T13:32:00
 */
trait Publisher[A]:

    val log: System.Logger = System.getLogger(getClass.getName)

    def created(x: A): Unit
    def created(xs: Iterable[A]): Unit =
        for x <- xs
        do created(x)
    def created(opt: Option[A]): Unit  = opt match
        case None    => // do nothing
        case Some(a) => Try(created(a)).recover {
            case e => log.atWarn.withCause(e).log(s"Failed to publish created message for $a")
        }

    def updated(x: A): Unit
    def updated(xs: Iterable[A]): Unit =
        for x <- xs
        do updated(x)
    def updated(opt: Option[A]): Unit = opt match {
        case None    => // do nothing
        case Some(a) => Try(updated(a)).recover {
            case e => log.atWarn.withCause(e).log(s"Failed to publish updated message for $a")
        }
    }

    def deleted(x: A): Unit
    def deleted(xs: Iterable[A]): Unit =
        for x <- xs
        do deleted(x)
    def deleted(opt: Option[A]): Unit = opt match {
        case None => // do nothing
        case Some(a) => Try(deleted(a)).recover {
            case e => log.atWarn.withCause(e).log(s"Failed to publish deleted message for $a")
        }
    }

/**
 * Decorator for an reactive Subject that publishes AnnotationMessages for common use cases.
 * @param subject
 */
class AnnotationPublisher(subject: Subject[Any]) extends Publisher[Annotation]:
    def created(annotation: Annotation): Unit   = Try:
        subject.onNext(AnnotationCreatedMessage(annotation))
        subject.onNext(NatsMessage.created(annotation))

    def created(observation: Observation): Unit = created(Annotation.from(observation.toEntity))

    def updated(annotation: Annotation): Unit = Try:
        subject.onNext(NatsMessage.updated(annotation))

    def updated(observation: Observation): Unit = Try:
        subject.onNext(NatsMessage.updated(observation))

    def deleted(annotation: Annotation): Unit = Try:
        subject.onNext(NatsMessage.deleted(annotation))

    def deleted(observation: Observation): Unit = Try:
        subject.onNext(NatsMessage.deleted(observation))

class Ob



class AssociationPublisher(subject: Subject[Any]) extends Publisher[Association]:
    def created(association: Association): Unit = Try(
        subject.onNext(AssociationCreatedMessage(association))
    )

    def updated(association: Association): Unit = ???

    def deleted(association: Association): Unit = ???
