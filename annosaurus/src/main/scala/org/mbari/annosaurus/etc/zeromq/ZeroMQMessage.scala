package org.mbari.annosaurus.etc.zeromq

import org.mbari.annosaurus.domain.{Annotation, Association}
import org.mbari.annosaurus.messaging.Message
import org.mbari.annosaurus.etc.circe.CirceCodecs.{*, given}


trait ZeroMQMessage[+T] extends Message[T]
/**
 * Send when a new annotation is created or an existing one is updated
 *
 * @param content
 */
case class AnnotationCreatedMessage(content: Annotation) extends ZeroMQMessage[Annotation]:

    override def hashCode(): Int =
        this.content.observationUuid.hashCode() +
            this.content.observationTimestamp.hashCode() * 3

    override def equals(obj: Any): Boolean =
        obj match
            case that: AnnotationCreatedMessage =>
                this.content.observationUuid == that.content.observationUuid &&
                    this.content.observationTimestamp == that.content.observationTimestamp
            case _                       => false

    override def toJson: String = content.stringify

case class AssociationCreatedMessage(content: Association) extends Message[Association]:
    //  override def hashCode(): Int = this.content.uuid.hashCode()
    //
    //  override def equals(obj: Any): Boolean = obj match {
    //    case that: AssociationMessage => this.content.uuid == that.content.uuid
    //    case _ => false
    //  }

    override def toJson: String =
        content.stringify

