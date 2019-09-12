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
import org.mbari.vars.annotation.model.simple.{ConcurrentRequest, ErrorMsg, MultiRequest}
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

  private[this] val repository = new JdbcRepository(daoFactory.entityManagerFactory)

  get("/videoreference/:uuid") {
    val uuid = params.getAs[UUID]("uuid").getOrElse(halt(BadRequest(
      body = "A video reference 'uuid' parameter is required")))
    val limit = params.getAs[Int]("limit")
    val offset = params.getAs[Int]("offset")
    val addData = params.getAs[Boolean]("data").getOrElse(false)
    Future({
      val annos = repository.findByVideoReferenceUuid(uuid, limit, offset, addData)
        .asJava
      toJson(annos)
    })
  }

  get("/images/videoreference/:uuid") {
    val uuid = params.getAs[UUID]("uuid").getOrElse(halt(BadRequest(
      body = "A video reference 'uuid' parameter is required")))
    val limit = params.getAs[Int]("limit")
    val offset = params.getAs[Int]("offset")
    Future({
      val images = repository.findImagesByVideoReferenceUuid(uuid, limit, offset)
        .asJava
      toJson(images)
    })
  }

  get("/concept/:concept") {
    val concept = params.get("concept").getOrElse(halt(BadRequest(
      body = "A 'concept' parameter is required")))
    val limit = params.getAs[Int]("limit")
    val offset = params.getAs[Int]("offset")
    val addData = params.getAs[Boolean]("data").getOrElse(false)
    Future({
      val annos = repository.findByConcept(concept, limit, offset, addData)
        .asJava
      toJson(annos)
    })
  }

  get("/concept/images/:concept") {
    val concept = params.get("concept").getOrElse(halt(BadRequest(
      body = "A 'concept' parameter is required")))
    val limit = params.getAs[Int]("limit")
    val offset = params.getAs[Int]("offset")
    val addData = params.getAs[Boolean]("data").getOrElse(false)
    Future({
      val annos = repository.findByConceptWithImages(concept, limit, offset, addData)
        .asJava
      toJson(annos)
    })
  }

  get("/imagedmoments/concept/images/:concept") {
    val concept = params.get("concept").getOrElse(halt(BadRequest(
      body = "A 'concept' parameter is required")))
    val limit = params.getAs[Int]("limit")
    val offset = params.getAs[Int]("offset")
    Future({
      val imagedMomentUuids = repository.findImagedMomentUuidsByConceptWithImages(concept, limit, offset)
          .asJava
      toJson(imagedMomentUuids)
    })
  }

  post("/concurrent") {
    request.getHeader("Content-Type") match {
      case "application/json" =>
        val b = request.body
        val limit = params.getAs[Int]("limit")
        val offset = params.getAs[Int]("offset")
        val concurrentRequest = fromJson(b, classOf[ConcurrentRequest])
        val addData = params.getAs[Boolean]("data").getOrElse(false)
        Future({
          val annos = repository.findByConcurrentRequest(concurrentRequest, limit, offset, addData)
            .asJava
          toJson(annos)
        })
      case _ =>
        halt(BadRequest(toJson(ErrorMsg(400, "Posts to /concurrent only accept a JSON body (i.e. Content-Type: application/json)"))))

    }
  }

  post("/multi") {
    request.getHeader("Content-Type") match {
      case "application/json" =>
        val b = request.body
        val limit = params.getAs[Int]("limit")
        val offset = params.getAs[Int]("offset")
        val multiRequest = fromJson(b, classOf[MultiRequest])
        val addData = params.getAs[Boolean]("data").getOrElse(false)
        Future({
          val annos = repository.findByMultiRequest(multiRequest, limit, offset, addData)
            .asJava
          toJson(annos)
        })
      case _ =>
        halt(BadRequest(toJson(ErrorMsg(400, "Posts to /multi only accept a JSON body (i.e. Content-Type: application/json)"))))

    }
  }




}
