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

import io.nats.client.Nats
import io.reactivex.rxjava3.subjects.PublishSubject
import org.mbari.annosaurus.etc.circe.CirceCodecs.given
import org.mbari.annosaurus.repository.jpa.TransactionNotifier
import org.mbari.annosaurus.repository.jpa.entity.{AssociationEntity, ObservationEntity}
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.{LinkedBlockingQueue, TimeUnit}

class NatsPublisherSuite extends munit.FunSuite:

    // --- Docker / NATS wiring (set up once for all tests) ---

    val container = new GenericContainer(DockerImageName.parse("nats:latest"))
    container.addExposedPort(4222)
    container.start()
    Runtime.getRuntime.addShutdownHook(new Thread(() => container.stop()))

    val natsUrl = s"nats://localhost:${container.getMappedPort(4222)}"
    val topic   = "test.annotations"

    // Dedicated subject so tests are isolated from the global EventBus
    val subject   = PublishSubject.create[Any]().toSerialized
    val config    = NatsConfig(url = natsUrl, enable = true, topic = topic)
    val publisher = NatsPublisher.autowire(Some(config), subject)
        .getOrElse(fail("NatsPublisher.autowire returned None"))

    // Subscriber that collects raw JSON from NATS into a queue
    val nc       = Nats.connect(natsUrl)
    val received = new LinkedBlockingQueue[String]()
    val dispatcher = nc.createDispatcher(msg =>
        received.offer(new String(msg.getData, StandardCharsets.UTF_8))
    )
    dispatcher.subscribe(topic)
    Thread.sleep(200) // let the subscription establish before tests run

    override def afterAll(): Unit =
        publisher.close()
        nc.close()

    // --- helpers ---

    /** Drains the queue before each test to prevent cross-test interference. */
    override def beforeEach(context: BeforeEach): Unit = received.clear()

    def pollMessage(timeoutMs: Long = 3000): Option[NatsMessage] =
        Option(received.poll(timeoutMs, TimeUnit.MILLISECONDS)).flatMap { json =>
            io.circe.parser.decode[NatsMessage](json).toOption
        }

    def fireEvent(action: TransactionNotifier.Action, clazz: Class[?], uuid: UUID): Unit =
        TransactionNotifier.getRxSubject().onNext(
            new TransactionNotifier.Message(action, clazz, uuid)
        )

    // --- tests ---

    test("publishes CREATED message when an ObservationEntity is persisted"):
        val uuid = UUID.randomUUID()
        fireEvent(TransactionNotifier.Action.CREATE, classOf[ObservationEntity], uuid)
        assertEquals(
            pollMessage(),
            Some(NatsMessage(NatsMessage.Actions.CREATED, NatsMessage.DataTypes.OBSERVATION, uuid))
        )

    test("publishes UPDATED message when an ObservationEntity is updated"):
        val uuid = UUID.randomUUID()
        fireEvent(TransactionNotifier.Action.UPDATE, classOf[ObservationEntity], uuid)
        assertEquals(
            pollMessage(),
            Some(NatsMessage(NatsMessage.Actions.UPDATED, NatsMessage.DataTypes.OBSERVATION, uuid))
        )

    test("publishes DELETED message when an ObservationEntity is removed"):
        val uuid = UUID.randomUUID()
        fireEvent(TransactionNotifier.Action.REMOVE, classOf[ObservationEntity], uuid)
        assertEquals(
            pollMessage(),
            Some(NatsMessage(NatsMessage.Actions.DELETED, NatsMessage.DataTypes.OBSERVATION, uuid))
        )

    test("publishes CREATED message when an AssociationEntity is persisted"):
        val uuid = UUID.randomUUID()
        fireEvent(TransactionNotifier.Action.CREATE, classOf[AssociationEntity], uuid)
        assertEquals(
            pollMessage(),
            Some(NatsMessage(NatsMessage.Actions.CREATED, NatsMessage.DataTypes.ASSOCIATION, uuid))
        )

    test("publishes UPDATED message when an AssociationEntity is updated"):
        val uuid = UUID.randomUUID()
        fireEvent(TransactionNotifier.Action.UPDATE, classOf[AssociationEntity], uuid)
        assertEquals(
            pollMessage(),
            Some(NatsMessage(NatsMessage.Actions.UPDATED, NatsMessage.DataTypes.ASSOCIATION, uuid))
        )

    test("publishes DELETED message when an AssociationEntity is removed"):
        val uuid = UUID.randomUUID()
        fireEvent(TransactionNotifier.Action.REMOVE, classOf[AssociationEntity], uuid)
        assertEquals(
            pollMessage(),
            Some(NatsMessage(NatsMessage.Actions.DELETED, NatsMessage.DataTypes.ASSOCIATION, uuid))
        )

    test("does not publish for unrecognised entity types"):
        fireEvent(TransactionNotifier.Action.CREATE, classOf[String], UUID.randomUUID())
        assertEquals(pollMessage(timeoutMs = 500), None)
