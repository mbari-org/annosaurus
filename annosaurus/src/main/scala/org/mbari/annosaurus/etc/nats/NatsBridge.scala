package org.mbari.annosaurus.etc.nats

import io.reactivex.rxjava3.subjects.Subject
import org.mbari.annosaurus.repository.jpa.TransactionMessenger
import org.mbari.annosaurus.repository.jpa.entity.{AssociationEntity, ObservationEntity}

class NatsBridge(source: Subject[?], sink: Subject[?]) {

    val log = System.getLogger(getClass.getName)

    def handleObservation(msg: TransactionMessenger.Message[ObservationEntity]): Unit = ???

    def handleAssociation(msg: TransactionMessenger.Message[AssociationEntity]): Unit = ???



}
