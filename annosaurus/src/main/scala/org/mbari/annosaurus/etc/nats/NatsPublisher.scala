package org.mbari.annosaurus.etc.nats


import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.Subject
import java.util.concurrent.{LinkedBlockingQueue, TimeUnit}
import scala.util.control.NonFatal
import org.mbari.annosaurus.NatsConfig

import org.mbari.annosaurus.etc.jdk.Loggers
import org.mbari.annosaurus.etc.jdk.Loggers.given
import org.mbari.annosaurus.messaging.{GenericMessage, MessageBus}

class NatsPublisher(val topic: String, val port: Int, val subject: Subject[?]) {

    private val queue                  = new LinkedBlockingQueue[GenericMessage[?]]()
    private val disposable: Disposable = MessageBus
        .RxSubject
        .ofType(classOf[GenericMessage[?]])
        .observeOn(Schedulers.io())
        .distinct()
        .subscribe(m => queue.offer(m))
    private val log                    = Loggers(getClass)
  
}

object NatsPublisher:

    private val log = Loggers(getClass)

    def apply(config: NatsConfig, subject: Subject[?]): Option[NatsPublisher] =
        if config.enable then Some(new NatsPublisher(config.subject, config.port, subject))
        else None


    def autowire(
        opt: Option[NatsConfig],
        subject: Subject[Any] = MessageBus.RxSubject
    ): Unit =
        for 
            config <- opt
            if config.enable
        yield new NatsPublisher(config.subject, config.port, subject)