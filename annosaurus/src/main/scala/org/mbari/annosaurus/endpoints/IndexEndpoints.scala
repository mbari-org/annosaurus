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

package org.mbari.annosaurus.endpoints

import org.mbari.annosaurus.controllers.IndexController

import scala.concurrent.{ExecutionContext, Future}
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint

import java.util.UUID
import org.mbari.annosaurus.domain.ImagedMoment
import org.mbari.annosaurus.etc.circe.CirceCodecs.{*, given}

class IndexEndpoints(controller: IndexController)(implicit val executor: ExecutionContext) extends Endpoints {

    val findByVideoReferenceUUID = openEndpoint
        .get
        .in(paging)
        .in("v1" / "videoreference" / path[UUID]("uuid"))
        .out(jsonBody[List[ImagedMoment]].description("The IndexEntity objects"))
    
    val findByVideoReferenceUUIDImpl = findByVideoReferenceUUID.serverLogic { (paging, uuid) =>
        val f = controller.findByVideoReferenceUUID(uuid, paging.limit, paging.offset)
            .map(xs => xs.map(ImagedMoment.from).toList)
        handleErrors(f)
    }
    
    val bulkUpdateRecordedTimestamps = openEndpoint
        .put
        .in("v1" / "tapetime")
        .in(jsonBody[List[ImagedMoment]].description("The IndexEntity objects"))
        .out(jsonBody[List[ImagedMoment]].description("The IndexEntity objects"))
    
    // val bulkUpdateRecordedTimestampsImpl = bulkUpdateRecordedTimestamps.serverLogic { indices =>
    //     val im = indices.map(ImagedMoment.from)
    //     val f = controller.bulkUpdateRecordedTimestamps(im)
    //     handleErrors(f)
    // }
    
     override def all: List[Endpoint[?, ?, ?, ?, ?]] = ???
//         List(findByVideoReferenceUUID, bulkUpdateRecordedTimestamps)
    
     override def allImpl: List[ServerEndpoint[Any, Future]] = ???
         //List(findByVideoReferenceUUIDImpl, bulkUpdateRecordedTimestampsImpl)
  
}
