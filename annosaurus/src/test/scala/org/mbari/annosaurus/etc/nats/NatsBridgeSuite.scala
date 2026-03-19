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

package org.mbari.annosaurus.etc.nats

import io.reactivex.rxjava3.subjects.PublishSubject
import org.mbari.annosaurus.repository.jpa.TransactionNotifier
import org.mbari.annosaurus.repository.jpa.entity.{AssociationEntity, ObservationEntity}

import java.util.UUID
import scala.collection.mutable

class NatsBridgeSuite extends munit.FunSuite:

    def makeSource = PublishSubject.create[Any]().toSerialized
    def makeMsg[T](action: TransactionNotifier.Action, clazz: Class[T], uuid: UUID) =
        new TransactionNotifier.Message[T](action, clazz, uuid)

    // --- handle() unit tests (pure, no Subject needed) ---

    test("handle returns CREATED/OBSERVATION for ObservationEntity CREATE"):
        val source = makeSource
        val bridge = NatsBridge(source, makeSource)
        val uuid   = UUID.randomUUID()
        val result = bridge.handle(makeMsg(TransactionNotifier.Action.CREATE, classOf[ObservationEntity], uuid))
        assertEquals(result, Some(NatsMessage(NatsMessage.Actions.CREATED, NatsMessage.DataTypes.OBSERVATION, uuid)))

    test("handle returns UPDATED/OBSERVATION for ObservationEntity UPDATE"):
        val source = makeSource
        val bridge = NatsBridge(source, makeSource)
        val uuid   = UUID.randomUUID()
        val result = bridge.handle(makeMsg(TransactionNotifier.Action.UPDATE, classOf[ObservationEntity], uuid))
        assertEquals(result, Some(NatsMessage(NatsMessage.Actions.UPDATED, NatsMessage.DataTypes.OBSERVATION, uuid)))

    test("handle returns DELETED/OBSERVATION for ObservationEntity REMOVE"):
        val bridge = NatsBridge(makeSource, makeSource)
        val uuid   = UUID.randomUUID()
        val result = bridge.handle(makeMsg(TransactionNotifier.Action.REMOVE, classOf[ObservationEntity], uuid))
        assertEquals(result, Some(NatsMessage(NatsMessage.Actions.DELETED, NatsMessage.DataTypes.OBSERVATION, uuid)))

    test("handle returns CREATED/ASSOCIATION for AssociationEntity CREATE"):
        val bridge = NatsBridge(makeSource, makeSource)
        val uuid   = UUID.randomUUID()
        val result = bridge.handle(makeMsg(TransactionNotifier.Action.CREATE, classOf[AssociationEntity], uuid))
        assertEquals(result, Some(NatsMessage(NatsMessage.Actions.CREATED, NatsMessage.DataTypes.ASSOCIATION, uuid)))

    test("handle returns UPDATED/ASSOCIATION for AssociationEntity UPDATE"):
        val bridge = NatsBridge(makeSource, makeSource)
        val uuid   = UUID.randomUUID()
        val result = bridge.handle(makeMsg(TransactionNotifier.Action.UPDATE, classOf[AssociationEntity], uuid))
        assertEquals(result, Some(NatsMessage(NatsMessage.Actions.UPDATED, NatsMessage.DataTypes.ASSOCIATION, uuid)))

    test("handle returns DELETED/ASSOCIATION for AssociationEntity REMOVE"):
        val bridge = NatsBridge(makeSource, makeSource)
        val uuid   = UUID.randomUUID()
        val result = bridge.handle(makeMsg(TransactionNotifier.Action.REMOVE, classOf[AssociationEntity], uuid))
        assertEquals(result, Some(NatsMessage(NatsMessage.Actions.DELETED, NatsMessage.DataTypes.ASSOCIATION, uuid)))

    test("handle returns None for unknown entity class"):
        val bridge = NatsBridge(makeSource, makeSource)
        val uuid   = UUID.randomUUID()
        val result = bridge.handle(makeMsg(TransactionNotifier.Action.CREATE, classOf[String], uuid))
        assertEquals(result, None)

    // --- subscription tests (source → sink wiring) ---

    test("messages on source are forwarded to sink"):
        val source   = makeSource
        val sink     = makeSource
        val received = mutable.Buffer[Any]()
        sink.subscribe(received.append(_))
        NatsBridge(source, sink)

        val uuid = UUID.randomUUID()
        source.onNext(makeMsg(TransactionNotifier.Action.CREATE, classOf[ObservationEntity], uuid))

        assertEquals(received.size, 1)
        assertEquals(received.head, NatsMessage(NatsMessage.Actions.CREATED, NatsMessage.DataTypes.OBSERVATION, uuid))

    test("non-Message items on source are filtered out"):
        val source   = makeSource
        val sink     = makeSource
        val received = mutable.Buffer[Any]()
        sink.subscribe(received.append(_))
        NatsBridge(source, sink)

        source.onNext("not a message")
        source.onNext(42)
        source.onNext(makeMsg(TransactionNotifier.Action.UPDATE, classOf[AssociationEntity], UUID.randomUUID()))

        assertEquals(received.size, 1)
