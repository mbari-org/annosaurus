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
import org.mbari.vars.annotation.model.simple.{ConcurrentRequest, Count, ErrorMsg, MultiRequest}
import org.scalatra.BadRequest

import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._

/**
  * @author Brian Schlining
  * @since 2019-07-22T11:33:00
  */
class FastAnnotationV1Api(daoFactory: JPADAOFactory)(implicit val executor: ExecutionContext)
    extends V1APIStack {

  before() {
    contentType = "application/json"
    response.headers.set("Access-Control-Allow-Origin", "*")
  }

  private[this] val repository = new JdbcRepository(daoFactory.entityManagerFactory)

  get("/") {
    params.getAs[Int]("limit").orElse(Some(5000))
    params.getAs[Int]("offset")

  }

  get("/count") {
    Future {
      val count = repository.countAll()
      toJson(Count(count))
    }
  }

  get("/videoreference/:uuid") {
    val uuid = params
      .getAs[UUID]("uuid")
      .getOrElse(
        halt(BadRequest(toJson(ErrorMsg(400, "A video reference 'uuid' parameter is required"))))
      )
    val limit   = params.getAs[Int]("limit")
    val offset  = params.getAs[Int]("offset")
    val addData = params.getAs[Boolean]("data").getOrElse(false)
    Future {
      val annos = repository.findByVideoReferenceUuid(uuid, limit, offset, addData).asJava
      toJson(annos)
    }
  }

  get("/images/videoreference/:uuid") {
    val uuid = params
      .getAs[UUID]("uuid")
      .getOrElse(
        halt(BadRequest(toJson(ErrorMsg(400, "A video reference 'uuid' parameter is required"))))
      )
    val limit  = params.getAs[Int]("limit")
    val offset = params.getAs[Int]("offset")
    Future {
      val images = repository.findImagesByVideoReferenceUuid(uuid, limit, offset).asJava
      toJson(images)
    }
  }

  get("/concept/:concept") {
    val concept =
      params
        .get("concept")
        .getOrElse(halt(BadRequest(toJson(ErrorMsg(400, "A 'concept' parameter is required")))))
    val limit   = params.getAs[Int]("limit")
    val offset  = params.getAs[Int]("offset")
    val addData = params.getAs[Boolean]("data").getOrElse(false)
    Future {
      val annos = repository.findByConcept(concept, limit, offset, addData).asJava
      toJson(annos)
    }
  }

  get("/concept/images/:concept") {
    val concept =
      params
        .get("concept")
        .getOrElse(halt(BadRequest(toJson(ErrorMsg(400, "A 'concept' parameter is required")))))
    val limit   = params.getAs[Int]("limit")
    val offset  = params.getAs[Int]("offset")
    val addData = params.getAs[Boolean]("data").getOrElse(false)
    Future {
      val annos = repository.findByConceptWithImages(concept, limit, offset, addData).asJava
      toJson(annos)
    }
  }

  get("/imagedmoments/concept/images/:concept") {
    val concept =
      params
        .get("concept")
        .getOrElse(halt(BadRequest(toJson(ErrorMsg(400, "A 'concept' parameter is required")))))
    val limit  = params.getAs[Int]("limit")
    val offset = params.getAs[Int]("offset")
    Future {
      val imagedMomentUuids =
        repository.findImagedMomentUuidsByConceptWithImages(concept, limit, offset).asJava
      toJson(imagedMomentUuids)
    }
  }

  get("/details/:link_name/:link_value") {
    val linkName = params
      .get("link_name")
      .getOrElse(halt(BadRequest(toJson(ErrorMsg(400, "A link_name parameter is required")))))
    val linkValue = params
      .get("link_value")
      .getOrElse(halt(BadRequest(toJson(ErrorMsg(400, "A link_value parameter is required")))))
    val addData = params.getAs[Boolean]("data").getOrElse(false)
    Future {
      val annos = repository.findByLinkNameAndLinkValue(linkName, linkValue, addData).asJava
      toJson(annos)
    }
  }

  delete("/videoreference/:uuid") {
    validateRequest()
    val uuid = params
      .getAs[UUID]("uuid")
      .getOrElse(
        halt(BadRequest(toJson(ErrorMsg(400, "A video reference 'uuid' parameter is required"))))
      )
    Future {
      val deleteCount = repository.deleteByVideoReferenceUuid(uuid)
      toJson(deleteCount)
    }
  }

  post("/concurrent") {
    request.getHeader("Content-Type") match {
      case "application/json" =>
        val b                 = request.body
        val limit             = params.getAs[Int]("limit")
        val offset            = params.getAs[Int]("offset")
        val concurrentRequest = fromJson(b, classOf[ConcurrentRequest])
        val addData           = params.getAs[Boolean]("data").getOrElse(false)
        Future {
          val annos =
            repository.findByConcurrentRequest(concurrentRequest, limit, offset, addData).asJava
          toJson(annos)
        }
      case _ =>
        halt(
          BadRequest(
            toJson(
              ErrorMsg(
                400,
                "Posts to /concurrent only accept a JSON body (i.e. Content-Type: application/json)"
              )
            )
          )
        )

    }
  }

  post("/multi") {
    request.getHeader("Content-Type") match {
      case "application/json" =>
        val b            = request.body
        val limit        = params.getAs[Int]("limit")
        val offset       = params.getAs[Int]("offset")
        val multiRequest = fromJson(b, classOf[MultiRequest])
        val addData      = params.getAs[Boolean]("data").getOrElse(false)
        Future {
          val annos = repository.findByMultiRequest(multiRequest, limit, offset, addData).asJava
          toJson(annos)
        }
      case _ =>
        halt(
          BadRequest(
            toJson(
              ErrorMsg(
                400,
                "Posts to /multi only accept a JSON body (i.e. Content-Type: application/json)"
              )
            )
          )
        )

    }
  }

}
