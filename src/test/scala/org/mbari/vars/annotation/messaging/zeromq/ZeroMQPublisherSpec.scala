package org.mbari.vars.annotation.messaging.zeromq

import java.time.Instant
import java.util.UUID

import org.mbari.vars.annotation.dao.jpa.AnnotationImpl
import org.mbari.vars.annotation.messaging.{AnnotationMessage, MessageBus}
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import org.zeromq.{SocketType, ZContext}
import zmq.ZMQ

/**
 * @author Brian Schlining
 * @since 2020-02-03T09:27:00
 */
class ZeroMQPublisherSpec extends FlatSpec with Matchers with BeforeAndAfterAll {

  val context = new ZContext()

  "ZeroMQPublisher" should "publish" in {
    val port = 9997
    val mq = new ZeroMQPublisher("test", port, MessageBus.RxSubject)

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
        while(ok) {
          val address = subscriber.recvStr()
          val contents = subscriber.recvStr()
          println(s"$address : $contents")
          count = count + 1
        }
      }
    })
    listenerThread.start()

    // Publish annotations
    val annotation = new AnnotationImpl
    annotation.concept = "foo"
    annotation.observationUuid = UUID.randomUUID()
    annotation.recordedTimestamp = Instant.now()
    MessageBus.RxSubject.onNext(AnnotationMessage(annotation))
    Thread.sleep(300)
    ok = false
    mq.close()
    count should be (1)

  }

  it should "publish many from multiple threads" in {
    val port = 9997
    val mq = new ZeroMQPublisher("test", port, MessageBus.RxSubject)

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
        while(ok) {
          val address = subscriber.recvStr()
          val contents = subscriber.recvStr()
          println(s"$address : $contents")
          count = count + 1
        }
      }
    })
    listenerThread.start()

    // Publish annotations
    for (i <- 0 until 1000) {
      val annotation = new AnnotationImpl
      annotation.concept = "bar" + i
      annotation.observationUuid = UUID.randomUUID()
      annotation.recordedTimestamp = Instant.now()
      val thread = new Thread(() => MessageBus.RxSubject.onNext(AnnotationMessage(annotation)))
      thread.setDaemon(true)
      thread.start()
    }

    Thread.sleep(1000) // Give messages a chance to propagate
    ok = false
    mq.close()
    count should be (1000)
  }

}
