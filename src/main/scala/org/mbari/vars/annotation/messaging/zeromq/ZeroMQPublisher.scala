package org.mbari.vars.annotation.messaging.zeromq

import java.util.concurrent.Executors

import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.Subject
import org.mbari.vars.annotation.Constants
import org.mbari.vars.annotation.messaging.{AnnotationMessage, MessageBus, PublisherMessage, Using}
import org.zeromq.{SocketType, ZContext, ZMQ}

import scala.concurrent.ExecutionContext

/**
 * @author Brian Schlining
 * @since 2020-01-30T15:47:00
 */
class ZeroMQPublisher(topic: String, port: Int, subject: Subject[PublisherMessage[Any]]) {

  private[this] val context = new ZContext()
  private[this] val publisher = context.createSocket(SocketType.PUB)
  publisher.bind(s"tcp://*:$port")

  private[this] val executor = Executors.newSingleThreadExecutor()
  private val disposable: Disposable = MessageBus.RxSubject
    .subscribeOn(Schedulers.from(executor))
    .ofType(classOf[AnnotationMessage])
    .subscribe(m => publish(m))

  def publish(m: AnnotationMessage): Unit = {
    val json = Constants.GSON.toJson(m.content)

  }

  def close(): Unit = {
    publisher.close()
    context.destroy()
    executor.shutdown()
    disposable.dispose()
  }


}
