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
import org.mbari.annosaurus.etc.zeromq.{AnnotationCreatedMessage, AssociationCreatedMessage}
import org.mbari.annosaurus.etc.jdk.Loggers.given
import org.mbari.annosaurus.etc.nats.NatsMessage
import org.mbari.annosaurus.repository.jpa.entity.AssociationEntity
import org.mbari.scommons.util.mail.liftToOption

import scala.util.Try

/**
 * @author
 *   Brian Schlining
 * @since 2020-03-04T13:32:00
 */
trait Publisher[A]:


    protected def tryWithLogging(f: => Unit): Unit = Try(f).recover {
        case e => log.atWarn.withCause(e).log("Failed to publish message")
    }

    val log: System.Logger = System.getLogger(getClass.getName)

    def created(x: A): Unit
    def created(xs: Iterable[A]): Unit =
        for x <- xs
        do created(x)
    def created(opt: Option[A]): Unit  = opt match
        case None    => // do nothing
        case Some(a) => created(a)

    def updated(x: A): Unit
    def updated(xs: Iterable[A]): Unit =
        for x <- xs
        do updated(x)
    def updated(opt: Option[A]): Unit = opt match {
        case None    => // do nothing
        case Some(a) =>  updated(a)
    }

    def deleted(x: A): Unit
    def deleted(xs: Iterable[A]): Unit =
        for x <- xs
        do deleted(x)
    def deleted(opt: Option[A]): Unit = opt match {
        case None => // do nothing
        case Some(a) => deleted(a)
    }

object Publisher:

    private def lookup[T](obj: T)(using subject: Subject[Any]): Publisher[T] =
        (obj match
            case _: Annotation      => AnnotationPublisher(subject)
            case _: Observation     => ObservationPublisher(subject)
            case _: AssociationEntity => AssociationPublisher(subject)
            case _                  => NoopPublisher(subject)
        ).asInstanceOf[Publisher[T]]

    def created[A](a: A)(using subject: Subject[Any]): Unit = lookup(a).created(a)
    def updated[A](a: A)(using subject: Subject[Any]): Unit = lookup(a).updated(a)
    def deleted[A](a: A)(using subject: Subject[Any]): Unit = lookup(a).deleted(a)

/**
 * Decorator for an reactive Subject that publishes AnnotationMessages for common use cases.
 * @param subject
 */
class AnnotationPublisher(subject: Subject[Any]) extends Publisher[Annotation]:
    def created(annotation: Annotation): Unit   = tryWithLogging({
        subject.onNext(AnnotationCreatedMessage(annotation))
        subject.onNext(NatsMessage.created(annotation))
    })


    def updated(annotation: Annotation): Unit =
        tryWithLogging(subject.onNext(NatsMessage.updated(annotation)))

    def deleted(annotation: Annotation): Unit =
        tryWithLogging(subject.onNext(NatsMessage.deleted(annotation)))


class ObservationPublisher(subject: Subject[Any]) extends Publisher[Observation]:
    def created(observation: Observation): Unit =
        tryWithLogging(subject.onNext(NatsMessage.created(observation)))

    def updated(observation: Observation): Unit = {
        Annotation.from(observation.toEntity).foreach(a => tryWithLogging(subject.onNext(AnnotationCreatedMessage(a))))
        tryWithLogging(subject.onNext(NatsMessage.updated(observation)))
    }

    def deleted(observation: Observation): Unit =
        tryWithLogging(subject.onNext(NatsMessage.deleted(observation)))

class AssociationPublisher(subject: Subject[Any]) extends Publisher[Association]:
    def created(association: Association): Unit =
        tryWithLogging({
            subject.onNext(AssociationCreatedMessage(association))
            subject.onNext(NatsMessage.created(association))
        })

    def updated(association: Association): Unit =
        tryWithLogging(subject.onNext(NatsMessage.updated(association)))


    def deleted(association: Association): Unit =
        tryWithLogging(subject.onNext(NatsMessage.deleted(association)))

class NoopPublisher(subject: Subject[Any]) extends Publisher[Any]:

    def created(x: Any): Unit = ()
    def updated(x: Any): Unit = ()
    def deleted(x: Any): Unit = ()