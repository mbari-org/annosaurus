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

import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.Subject
import org.mbari.annosaurus.messaging.{GenericMessage, MessageBus}
import org.mbari.annosaurus.ZeroMQConfig
import org.mbari.annosaurus.etc.jdk.Logging
import org.mbari.annosaurus.etc.jdk.Logging.{*, given}
import org.zeromq.{SocketType, ZContext}

import java.util.concurrent.{LinkedBlockingQueue, TimeUnit}
import scala.util.control.NonFatal

/** @author
  *   Brian Schlining
  * @since 2020-01-30T15:47:00
  */
class ZeroMQPublisher(val topic: String, val port: Int, val subject: Subject[?]) {

    private val context                = new ZContext()
    private val queue                  = new LinkedBlockingQueue[GenericMessage[?]]()
    private val disposable: Disposable = MessageBus
        .RxSubject
        .ofType(classOf[GenericMessage[?]])
        .observeOn(Schedulers.io())
        .distinct()
        .subscribe(m => queue.offer(m))
    private val log                          = Logging(getClass)

    @volatile
    var ok     = true
    val thread = new Thread(
        new Runnable {
            private val publisher = context.createSocket(SocketType.PUB)
            publisher.bind(s"tcp://*:$port")

            override def run(): Unit = while (ok) {
                try {
                    val msg = queue.poll(3600L, TimeUnit.SECONDS)
                    if (msg != null) {
                        val json = msg.toJson
                        publisher.sendMore(topic)
                        publisher.send(json)
                    }
                }
                catch {
                    case NonFatal(e) =>
                        log.atWarn
                            .withCause(e)
                            .log("An exception was thrown in ZeroMQPublishers publish thread")
                }
            }
        },
        "ZeroMQPublisher"
    )
    thread.setDaemon(true)
    thread.start()

    def close(): Unit = {
        context.destroy()
        disposable.dispose()
        ok = false
    }

}

object ZeroMQPublisher {

    private val log = Logging(getClass)

    /** @param opt
      *   The ZeroMQ config infor. The Config parser may not contain info for ZeroMQ. If it doesn't
      *   it returns None.
      * @param subject
      *   The message bus for zeromq to listen to
      * @return
      *   An option with a wired and active ZeroMQ publisher that will publish new or updated
      *   annotations as they happen.
      */
    def autowire(
        opt: Option[ZeroMQConfig],
        subject: Subject[Any] = MessageBus.RxSubject
    ): Option[ZeroMQPublisher] =
        for {
            conf <- opt
            if conf.enable
        } yield new ZeroMQPublisher(conf.topic, conf.port, subject)

    /** Logs info about the ZMQ configuration
      * @param opt
      */
    def log(opt: Option[ZeroMQPublisher]): Unit = opt match {
        case None    => log.atInfo.log("ZeroMQ is not enabled/configured")
        case Some(z) =>
            log.atInfo
                .log(
                    s"ZeroMQ is publishing annotations on port ${z.port} using topic '${z.topic}''"
                )
    }

}
