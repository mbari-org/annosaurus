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
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.Subject
import org.mbari.annosaurus.etc.jdk.Loggers
import org.mbari.annosaurus.etc.jdk.Loggers.given
import org.mbari.annosaurus.etc.rxjava.EventBus

import java.nio.charset.StandardCharsets
import java.util.concurrent.{LinkedBlockingQueue, TimeUnit}
import scala.util.control.NonFatal

class NatsPublisher(val url: String, val topic: String, val subject: Subject[?]):

    private val log                    = Loggers(getClass)
    private val nc                     = Nats.connect(url)
    private val queue                  = new LinkedBlockingQueue[NatsMessage]()
    private val disposable: Disposable = subject
        .ofType(classOf[NatsMessage])
        .observeOn(Schedulers.io())
        .distinct()
        .subscribe(m => queue.offer(m))

    @volatile
    var ok = true

    val thread = new Thread(
        () => while ok do
            try
                val msg = queue.poll(3600L, TimeUnit.SECONDS)
                if msg != null then
                    nc.publish(topic, msg.toJson.getBytes(StandardCharsets.UTF_8))
            catch
                case NonFatal(e) =>
                    log.atWarn
                        .withCause(e)
                        .log("An exception was thrown in NatsPublisher's publish thread")
        ,
        "NatsPublisher"
    )
    thread.setDaemon(true)
    thread.start()

    def close(): Unit =
        nc.close()
        disposable.dispose()
        ok = false

object NatsPublisher:

    private val log = Loggers(getClass)

    def autowire(
        opt: Option[NatsConfig],
        subject: Subject[Any] = EventBus.RxSubject
    ): Option[NatsPublisher] =
        for
            config <- opt
            if config.enable
        yield new NatsPublisher(config.url, config.topic, subject)

    def log(opt: Option[NatsPublisher]): Unit = opt match
        case None    => log.atInfo.log("NATS is not enabled/configured")
        case Some(p) =>
            log.atInfo.log(s"NATS is publishing annotations to '${p.topic}' at '${p.url}'")
