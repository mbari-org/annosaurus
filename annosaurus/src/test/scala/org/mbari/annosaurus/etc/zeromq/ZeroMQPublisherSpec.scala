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

import org.mbari.annosaurus.messaging.{AnnotationMessage, AssociationMessage, MessageBus}
import org.mbari.annosaurus.repository.jpa.entity.{AssociationEntity, ObservationEntity}
import org.mbari.annosaurus.repository.jpa.AnnotationImpl
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.zeromq.{SocketType, ZContext}
import zmq.ZMQ

import java.time.Instant
import java.util.UUID

/**
  * @author Brian Schlining
  * @since 2020-02-03T09:27:00
  */
class ZeroMQPublisherSpec extends AnyFlatSpec with Matchers with BeforeAndAfterAll {

  val context = new ZContext()

  "ZeroMQPublisher" should "publish annotations" in {
    val port = 9997
    val mq   = new ZeroMQPublisher("test", port, MessageBus.RxSubject)

    // Counts messages recieved
    @volatile
    var count = 0

    @volatile
    var ok = true

    val listenerThread = new Thread(new Runnable {

      val subscriber = context.createSocket(SocketType.SUB)
      subscriber.connect(s"tcp://localhost:$port")
      subscriber.subscribe(mq.topic.getBytes(ZMQ.CHARSET))

      override def run(): Unit = {
        while (ok) {
          val address  = subscriber.recvStr()
          val contents = subscriber.recvStr()
          println(s"$address : $contents")
          count = count + 1
          ok = false
        }
      }
    })
    listenerThread.start()
    Thread.sleep(200) // Give the thread above time to get set up.

    // Publish annotations
    val annotation = new AnnotationImpl
    annotation.concept = "foo"
    annotation.observationUuid = UUID.randomUUID()
    annotation.observationTimestamp = Instant.now()
    annotation.recordedTimestamp = Instant.now()
    val thread = new Thread(() =>
      MessageBus
        .RxSubject
        .onNext(AnnotationMessage(annotation))
    )
    thread.run()
    Thread.sleep(1000)
    mq.close()
    count should be(1)

  }

  it should "publish many annotations from multiple threads" in {
    val port = 9997
    val mq   = new ZeroMQPublisher("test", port, MessageBus.RxSubject)

    // Counts messages recieved
    @volatile
    var count = 0

    @volatile
    var ok = true

    val listenerThread = new Thread(new Runnable {

      private val subscriber = context.createSocket(SocketType.SUB)
      subscriber.connect(s"tcp://localhost:$port")
      subscriber.subscribe(mq.topic.getBytes(ZMQ.CHARSET))

      override def run(): Unit = {
        while (ok) {
          val address  = subscriber.recvStr()
          val contents = subscriber.recvStr()
//          println(s"$address : $contents")
          count = count + 1
        }
      }
    })
    listenerThread.start()
    Thread.sleep(200) // Give the thread above time to get set up.

    // Publish annotations
    for (i <- 0 until 1000) {
      val annotation = new AnnotationImpl
      annotation.concept = "bar" + i
      annotation.observationUuid = UUID.randomUUID()
      annotation.recordedTimestamp = Instant.now()
      annotation.observationTimestamp = Instant.now()
      val thread = new Thread(() => MessageBus.RxSubject.onNext(AnnotationMessage(annotation)))
      thread.setDaemon(true)
      thread.start()
    }

    Thread.sleep(2000) // Give messages a chance to propagate
    ok = false
    mq.close()
    count should be(1000)
  }

  it should "publish associations" in {
    val port = 9997
    val mq   = new ZeroMQPublisher("test", port, MessageBus.RxSubject)

    // Counts messages recieved
    @volatile
    var count = 0

    @volatile
    var ok = true

    val listenerThread = new Thread(new Runnable {

      val subscriber = context.createSocket(SocketType.SUB)
      subscriber.connect(s"tcp://localhost:$port")
      subscriber.subscribe(mq.topic.getBytes(ZMQ.CHARSET))

      override def run(): Unit = {
        while (ok) {
          val address  = subscriber.recvStr()
          val contents = subscriber.recvStr()
          println(s"$address : $contents")
          count = count + 1
          ok = false
        }
      }
    })
    listenerThread.start()
    Thread.sleep(200) // Give the thread above time to get set up.

    // Publish association
    val observation = ObservationEntity("foo")
    observation.uuid = UUID.randomUUID()
    val association = AssociationEntity("test", "self", "foo", "text/plain")
    association.uuid = UUID.randomUUID()
    observation.addAssociation(association)
    val thread = new Thread(() =>
      MessageBus
        .RxSubject
        .onNext(AssociationMessage(association))
    )
    thread.run()
    Thread.sleep(1000)
    mq.close()
    count should be(1)

  }

}
