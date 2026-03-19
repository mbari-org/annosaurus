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

import io.reactivex.rxjava3.subjects.Subject
import org.mbari.annosaurus.repository.jpa.TransactionNotifier
import org.mbari.annosaurus.repository.jpa.entity.{AssociationEntity, ObservationEntity}

/**
 * Translates JPA TransactionNotifier messages received from source into NATS messages. Then forwards the NATS messages
 * to the sink.
 *
 * @author Brian Schlining
 * @since 2026-03-18
 * @param source The source of messages, typically the TransactionNotifier
 * @param sink The sink of messages, typically the EventBus.RxSubject
 */
class NatsBridge(source: Subject[?], val sink: Subject[? >: NatsMessage]) extends AutoCloseable {

    private val log = System.getLogger(getClass.getName)
    private val observationClass = classOf[ObservationEntity]
    private val associationClass = classOf[AssociationEntity]

    private val disposable = source
        .ofType(classOf[TransactionNotifier.Message[?]])
        .subscribe(msg => handle(msg).foreach(sink.onNext))


    def handle(msg: TransactionNotifier.Message[?]): Option[NatsMessage] = {

        val action = msg.action() match
            case TransactionNotifier.Action.CREATE  => Some(NatsMessage.Actions.CREATED)
            case TransactionNotifier.Action.UPDATE  => Some(NatsMessage.Actions.UPDATED)
            case TransactionNotifier.Action.REMOVE  => Some(NatsMessage.Actions.DELETED)
            case _ => None

        val clazz = msg.clazz()
        val dataType =
            if observationClass.isAssignableFrom(clazz) then
                Some(NatsMessage.DataTypes.OBSERVATION)
            else if associationClass.isAssignableFrom(clazz) then
                Some(NatsMessage.DataTypes.ASSOCIATION)
            else
                None

        for
            a <- action
            d <- dataType
        yield
            NatsMessage(a, d, msg.uuid())
    }


    override def close(): Unit = disposable.dispose()


}
