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

import io.reactivex.rxjava3.subjects.PublishSubject
import org.mbari.annosaurus.domain.{Annotation, Association, Localization}
import org.mbari.annosaurus.etc.circe.CirceCodecs.{*, given}
import org.mbari.annosaurus.etc.zeromq.{AnnotationCreatedMessage, AssociationCreatedMessage, LocalizationCreatedMessage}

import scala.collection.mutable.ListBuffer

class PublisherSuite extends munit.FunSuite:

    private def fixture(): (Publisher, ListBuffer[Any]) =
        val subject  = PublishSubject.create[Any]().toSerialized
        val received = ListBuffer.empty[Any]
        subject.subscribe(x => received += x)
        (Publisher(subject), received)

    private val box = Localization("bounding_box", x = Some(0.1), y = Some(0.1), width = Some(0.2), height = Some(0.2))

    test("created(localization) emits exactly one LocalizationCreatedMessage and nothing else"):
        val (publisher, received) = fixture()
        publisher.created(box)
        assertEquals(received.toList, List(LocalizationCreatedMessage(box)))

    test("LocalizationCreatedMessage serializes the localization to JSON"):
        val json = LocalizationCreatedMessage(box).toJson
        // Like the other ZeroMQ messages, the payload is the camelCase domain JSON
        assertEquals(json.reify[Localization].toOption, Some(box))

    test("created(association) still emits an AssociationCreatedMessage"):
        val (publisher, received) = fixture()
        val association           = Association("link", "to", "value")
        publisher.created(association)
        assertEquals(received.toList, List(AssociationCreatedMessage(association)))

    test("created(unknown) emits nothing"):
        val (publisher, received) = fixture()
        publisher.created("not a domain object")
        assert(received.isEmpty)

    test("created(annotation) emits an AnnotationCreatedMessage whose payload carries the localizations"):
        val (publisher, received) = fixture()
        val annotation            = Annotation(concept = Some("Nanomia"), localizations = Seq(box))
        publisher.created(annotation)
        assertEquals(received.size, 1)
        received.head match
            case m: AnnotationCreatedMessage =>
                assertEquals(m.content.localizations, Seq(box))
                assert(m.toJson.contains("localizations"), m.toJson)
            case other                       => fail(s"Unexpected message: $other")
