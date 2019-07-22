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

import java.util.UUID

import org.mbari.vars.annotation.dao.jdbc.JdbcRepository
import org.mbari.vars.annotation.dao.jpa.JPADAOFactory
import org.scalatra.BadRequest

import scala.concurrent.{ExecutionContext, Future}
import scala.collection.JavaConverters._

/**
 * @author Brian Schlining
 * @since 2019-07-22T11:33:00
 */
class FastAnnotationV1Api(daoFactory: JPADAOFactory)
                         (implicit val executor: ExecutionContext) extends V1APIStack {

  before() {
    contentType = "application/json"
    response.headers += ("Access-Control-Allow-Origin" -> "*")
  }

  get("/videoreferenceuuid/:uuid") {
    val uuid = params.getAs[UUID]("uuid").getOrElse(halt(BadRequest(
      body = "A video reference 'uuid' parameter is required")))
    val limit = params.getAs[Int]("limit")
    val offset = params.getAs[Int]("offset")
    Future({
      val repo = new JdbcRepository(daoFactory.entityManagerFactory.createEntityManager())
      val annos = repo.findByVideoReferenceUuid(uuid, limit, offset)
        .asJava
      toJson(annos)
    })
  }


}
