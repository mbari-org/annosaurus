package org.mbari.annosaurus.etc.nats

import org.mbari.annosaurus.domain.{Annotation, Association, Observation}
import org.mbari.annosaurus.messaging.Message
import org.mbari.annosaurus.etc.circe.CirceCodecs.{*, given}

import java.util.UUID

case class NatsMessage(
                          action: String,
                          dataType: String,
                          uuid: UUID) extends Message[UUID]{

    override def content: UUID = uuid

    override def toJson: String = this.stringify
}

object NatsMessage:

    private val deleted: String = "deleted"
    private val created: String = "created"
    private val updated: String = "updated"

    def created[A](obj: A): Option[NatsMessage] = from(created, obj)

    def updated[A](obj: A): Option[NatsMessage] = from(updated, obj)

    def deleted[A](obj: A): Option[NatsMessage] = from(deleted, obj)

    private def from[A](action: String, obj: A): Option[NatsMessage] =
        obj match {
            case a: Annotation => a.observationUuid.map(NatsMessage(action, "observation", _))
            case o: Observation => o.uuid.map(NatsMessage(action, "observation", _))
            case b: Association => b.uuid.map(NatsMessage(action, "association", _))
            case _ => None
        }



