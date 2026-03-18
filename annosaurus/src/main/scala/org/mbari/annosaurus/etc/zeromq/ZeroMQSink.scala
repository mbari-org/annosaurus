package org.mbari.annosaurus.etc.zeromq

import io.reactivex.rxjava3.subjects.Subject
import org.mbari.annosaurus.domain.Annotation
import org.mbari.annosaurus.repository.jpa.TransactionMessenger
import org.mbari.annosaurus.repository.jpa.entity.{AssociationEntity, ObservationEntity}
import org.mbari.annosaurus.etc.jdk.Loggers.given

import scala.util.Try

class ZeroMQBridge[T](source: Subject[T], sink: Subject[?]) {


    val log: System.Logger = System.getLogger(getClass.getName)

    protected def tryWithLogging(f: => Unit): Unit = Try(f).recover {
        case e => log.atWarn.withCause(e).log("Failed to publish message")
    }


    def handleObservation(msg: TransactionMessenger.Message[ObservationEntity]): Unit =
        tryWithLogging({
//            val entity = msg.
//            val annotation = Annotation.from(entity)

        })


    def handleAssociation(msg: TransactionMessenger.Message[AssociationEntity]): Unit = ???

}
