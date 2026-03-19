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

import org.mbari.annosaurus.domain.{Annotation, Association, Observation}
import org.mbari.annosaurus.messaging.Message
import org.mbari.annosaurus.etc.circe.CirceCodecs.{*, given}

import java.util.UUID

/**
 * A minimal message that contains the action, data type, and uuid of the affected record. This is used to trigger
 * updates in other services without sending the entire record. The receiving service can then query for the
 * record using the uuid.
 *
 * @param action 1 of CREATED, UPDATED, or DELETED
 * @param dataType 1 of OBSERVATION or ASSOCIATION
 * @param uuid The uuid of the record that was affected
 */
case class NatsMessage(action: String, dataType: String, uuid: UUID) extends Message[UUID]:

    override def content: UUID = uuid

    override def toJson: String = this.stringify

object NatsMessage:

    enum Actions:
        case DELETED, CREATED, UPDATED

    enum DataTypes:
        case OBSERVATION, ASSOCIATION

    /**
     * Factory method for creating a NatsMessage.
     * @param action 1 of CREATED, UPDATED, or DELETED
     * @param dataType 1 of OBSERVATION or ASSOCIATION
     * @param uuid The uuid of the record that was affected
     * @return A NatsMessage
     */
    def apply(action: Actions, dataType: DataTypes, uuid: UUID): NatsMessage =
        NatsMessage(action.toString, dataType.toString, uuid)
