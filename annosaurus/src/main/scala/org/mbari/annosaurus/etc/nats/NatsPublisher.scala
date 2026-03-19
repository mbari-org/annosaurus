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
import org.mbari.annosaurus.repository.jpa.TransactionNotifier

import java.nio.charset.StandardCharsets
import java.util.concurrent.{LinkedBlockingQueue, TimeUnit}
import scala.util.Try
import scala.util.control.NonFatal

class NatsPublisher(val url: String, val topic: String, val source: Subject[?], closeOp: () => Unit = () => {}):

    private val log                    = Loggers(getClass)
    @volatile private var nc           = Nats.connect(url)
    private val queue                  = new LinkedBlockingQueue[NatsMessage]()
    private val disposable: Disposable = source
        .ofType(classOf[NatsMessage])
        .subscribeOn(Schedulers.io())
        .distinct()
        .subscribe(m => queue.offer(m))

    @volatile var ok = true

    val thread = new Thread(
        () =>
            while ok do
                try
                    val msg = queue.poll(3600L, TimeUnit.SECONDS)
                    if msg != null then publish(msg)
                catch
                    case NonFatal(e) =>
                        log.atWarn
                            .withCause(e)
                            .log("Unexpected error in NatsPublisher thread")
        ,
        "NatsPublisher"
    )
    thread.setDaemon(true)
    thread.start()

    private def publish(msg: NatsMessage): Unit =
        try
            nc.publish(topic, msg.toJson.getBytes(StandardCharsets.UTF_8))
        catch
            case NonFatal(e) =>
                log.atWarn.withCause(e).log("Failed to publish to NATS, requeueing message and reconnecting")
                queue.offer(msg)
                reconnect()

    private def reconnect(): Unit =
        var delay    = 1000L
        var connected = false
        while !connected && ok do
            Try(nc.close())
            try
                log.atInfo.log(s"Attempting to reconnect to NATS at $url ...")
                nc = Nats.connect(url)
                connected = true
                log.atInfo.log("Reconnected to NATS successfully")
            catch
                case NonFatal(e) =>
                    log.atWarn.withCause(e).log(s"Reconnect failed, retrying in ${delay}ms")
                    Thread.sleep(delay)
                    delay = Math.min(delay * 2, 30000L)

    def close(): Unit =
        ok = false
        Try(nc.close())
        disposable.dispose()
        closeOp()

object NatsPublisher:

    private val log = Loggers(getClass)

    /**
     * Creates a NatsPublisher if the config contains NATS info. Otherwise returns None.
     *
     * @param opt The NATS config infor. The Config parser may not contain info for NATS. If it doesn't it returns None.
     * @param subject The RX subjectd to listen for NatsMessages. This is typically the EventBus.RxSubject but can be overridden for testing.
     *                NatsMessage are converted to JSON and published to NATS.
     * @return
     */
    def autowire(
        opt: Option[NatsConfig],
        subject: Subject[Any] = EventBus.RxSubject
    ): Option[NatsPublisher] = {
        val x = try
            for
                config <- opt
                if config.enable
            yield
                val source = TransactionNotifier.getRxSubject

                // Translates TransactionNotifier messages to NatsMessages and forwards to subject
                val bridge = new NatsBridge(source, subject)

                val closeOp = () => bridge.close()
                new NatsPublisher(config.url, config.topic, subject, closeOp)
        catch
            case NonFatal(e) =>
                log.atError.withCause(e).log("Failed to initialize NATS publisher")
                None
        log(x)
        x
    }

    def log(opt: Option[NatsPublisher]): Unit = opt match
        case None    => log.atInfo.log("NATS is not enabled/configured")
        case Some(p) =>
            log.atInfo.log(s"NATS is publishing annotations to '${p.url}' using topic '${p.topic}'")
