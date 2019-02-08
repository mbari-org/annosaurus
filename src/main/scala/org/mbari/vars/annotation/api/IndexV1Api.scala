package org.mbari.vars.annotation.api

import java.util.UUID

import org.mbari.vars.annotation.controllers.IndexController
import org.mbari.vars.annotation.dao.jpa.IndexImpl
import org.scalatra.BadRequest

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext


/**
  * @author Brian Schlining
  * @since 2019-02-08T11:00:00
  */
class IndexV1Api(controller: IndexController)(implicit val executor: ExecutionContext)
  extends APIStack {

  before() {
    contentType = "application/json"
    response.headers += ("Access-Control-Allow-Origin" -> "*")
  }

  get("/videoreference/:uuid") {
    val uuid = params.getAs[UUID]("uuid").getOrElse(halt(BadRequest("Please provide a Video Reference UUID")))
    val limit = params.getAs[Int]("limit")
    val offset = params.getAs[Int]("offset")
    controller.findByVideoReferenceUUID(uuid, limit, offset)
      .map(_.asJava)
      .map(toJson)
  }

  put("/tapetime") {
    validateRequest()
    request.getHeader("Content-Type") match {
      case "application/json" =>
        val indicies = fromJson(request.body, classOf[Array[IndexImpl]])
        controller.bulkUpdateRecordedTimestamps(indicies)
          .map(_.asJava)
          .map(toJson)
      case _ =>
        val m = Map("error" -> "Puts to tapetime only accept JSON body (i.e. Content-Type: application/json)").asJava
        halt(BadRequest(toJson(m)))
    }
  }

}
