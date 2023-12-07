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

package org.mbari.vars.annotation.api.v1

import org.mbari.vars.annotation.repository.jdbc.AnalysisRepository
import org.mbari.vars.annotation.repository.jpa.JPADAOFactory
import org.mbari.vars.annotation.model.simple.{ErrorMsg, QueryConstraints, QueryConstraintsResponse}
import org.scalatra.BadRequest

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class AnalysisV1Api(daoFactory: JPADAOFactory)(implicit val executor: ExecutionContext)
    extends V1APIStack {

  before() {
    contentType = "application/json"
  }

  private[this] val repository = new AnalysisRepository(daoFactory.entityManagerFactory)

  post("/histogram/depth") {
    val body          = request.body
    val binSizeMeters = params.getAs[Int]("size").getOrElse(50)
    Try(QueryConstraints.fromJson(body)) match {
      case Success(constraints) =>
        Future {
          val hist     = repository.depthHistogram(constraints, binSizeMeters)
          val response = QueryConstraintsResponse(constraints, hist)
          toJson(response)
        }
      case Failure(_) =>
        halt(BadRequest(toJson(ErrorMsg(400, "valid query constraints are required"))))
    }
  }

  post("/histogram/time") {
    val body        = request.body
    val binSizeDays = params.getAs[Int]("size").getOrElse(30)
    Try(QueryConstraints.fromJson(body)) match {
      case Success(constraints) =>
        Future {
          val hist     = repository.timeHistogram(constraints, binSizeDays)
          val response = QueryConstraintsResponse(constraints, hist)
          toJson(response)
        }
      case Failure(_) =>
        halt(BadRequest(toJson(ErrorMsg(400, "valid query constraints are required"))))
    }
  }

}
