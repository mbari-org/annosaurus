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
import org.mbari.annosaurus.repository.jpa.entity.AssociationEntity
import org.mbari.scommons.util.mail.liftToOption

import scala.util.Try

/**
 * @author
 *   Brian Schlining
 * @since 2020-03-04T13:32:00
 */
class Publisher(subject: Subject[? >: Message[?]]):


    protected def tryWithLogging(f: => Unit): Unit = Try(f).recover {
        case e => log.atWarn.withCause(e).log("Failed to publish message")
    }

    val log: System.Logger = System.getLogger(getClass.getName)

    def created(x: Any): Unit =
        x match
            case annotation: Annotation => tryWithLogging(subject.onNext(AnnotationCreatedMessage(annotation)))
            case observation: Observation => tryWithLogging(subject.onNext(AnnotationCreatedMessage(Annotation.from(observation.toEntity))))
            case association: Association => tryWithLogging(subject.onNext(AssociationCreatedMessage(association)))
            case _ => ()

    def created(xs: Iterable[Any]): Unit =
        for x <- xs
        do created(x)
    def created(opt: Option[Any]): Unit  = opt match
        case None    => // do nothing
        case Some(a) => created(a)
