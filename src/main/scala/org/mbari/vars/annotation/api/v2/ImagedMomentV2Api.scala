package org.mbari.vars.annotation.api.v2

import java.time.Instant
import java.util.UUID

import org.mbari.vars.annotation.api.APIStack
import org.mbari.vars.annotation.controllers.ImagedMomentController
import org.mbari.vars.annotation.model.ImagedMoment
import org.mbari.vars.annotation.util.ResponseUtilities
import org.scalatra.BadRequest

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext

/**
  * @author Brian Schlining
  * @since 2019-05-08T10:02:00
  */
class ImagedMomentV2Api(controller: ImagedMomentController)(implicit val executor: ExecutionContext)
  extends APIStack {

  before() {
    contentType = "application/json"
    response.headers += ("Access-Control-Allow-Origin" -> "*")
  }

  get("/videoreference/:uuid") {
    val uuid = params.getAs[UUID]("uuid").getOrElse(halt(BadRequest("Please provide a Video Reference UUID")))
    val limit = params.getAs[Int]("limit")
    val offset = params.getAs[Int]("offset")

    val (closeable, stream) = controller.streamByVideoReferenceUUID(uuid, limit, offset)
    ResponseUtilities.sendStreamedResponse(response, stream, (im: ImagedMoment) => toJson(im))
    closeable.close()
    Unit
  }

  get("/videoreferences/modified/:start") {
    val start = params.getAs[Instant]("start").getOrElse(halt(BadRequest("Please provide a start date (yyyy-mm-ddThh:mm:ssZ)")))
    val end = Instant.now()
    val limit = params.getAs[Int]("limit")
    val offset = params.getAs[Int]("offset")
    val (closeable, stream) = controller.streamVideoReferenceUuidsBetweenUpdatedDates(start, end, limit, offset)
    val json = toJson(stream.iterator()
      .asScala
      .toSeq
      .map(_.toString)
      .asJava)
    closeable.close()
    json
  }

  get("/videoreferences/modified/:start/:end") {
    val start = params.getAs[Instant]("start").getOrElse(halt(BadRequest("Please provide a start date (yyyy-mm-ddThh:mm:ssZ)")))
    val end = params.getAs[Instant]("end").getOrElse(halt(BadRequest("Please provide an end date (yyyy-mm-ddThh:mm:ssZ)")))
    val limit = params.getAs[Int]("limit")
    val offset = params.getAs[Int]("offset")
    val (closeable, stream) = controller.streamVideoReferenceUuidsBetweenUpdatedDates(start, end, limit, offset)
    val json = toJson(stream.iterator()
        .asScala
        .toSeq
        .map(_.toString)
        .asJava)
    closeable.close()
    json
  }

  get("/modified/:start") {
    val start = params.getAs[Instant]("start").getOrElse(halt(BadRequest("Please provide a start date (yyyy-mm-ddThh:mm:ssZ)")))
    val end = Instant.now()
    val limit = params.getAs[Int]("limit")
    val offset = params.getAs[Int]("offset")

    val (closeable, stream) = controller.streamBetweenUpdatedDates(start, end, limit, offset)
    ResponseUtilities.sendStreamedResponse(response, stream, (im: ImagedMoment) => toJson(im))
    closeable.close()
    Unit
  }

  get("/modified/:start/:end") {
    val start = params.getAs[Instant]("start").getOrElse(halt(BadRequest("Please provide a start date (yyyy-mm-ddThh:mm:ssZ)")))
    val end = params.getAs[Instant]("end").getOrElse(halt(BadRequest("Please provide an end date (yyyy-mm-ddThh:mm:ssZ)")))
    val limit = params.getAs[Int]("limit")
    val offset = params.getAs[Int]("offset")

    val (closeable, stream) = controller.streamBetweenUpdatedDates(start, end, limit, offset)
    ResponseUtilities.sendStreamedResponse(response, stream, (im: ImagedMoment) => toJson(im))
    closeable.close()
    Unit
  }

  get("/concept/:name") {
    val name = params.get("name")
      .getOrElse(halt(BadRequest("""{"reason": "Please provide a concept name"}""")))
    val limit = params.getAs[Int]("limit")
    val offset = params.getAs[Int]("offset")
    val (closeable, stream) = controller.streamByConcept(name, limit, offset)
    ResponseUtilities.sendStreamedResponse(response, stream, (im: ImagedMoment) => toJson(im))
    closeable.close()
    Unit
  }


//  get("/modified/:start/:end") {
//    val start = params.getAs[Instant]("start").getOrElse(halt(BadRequest("Please provide a start date (yyyy-mm-ddThh:mm:ssZ)")))
//    val end = params.getAs[Instant]("end").getOrElse(halt(BadRequest("Please provide an end date (yyyy-mm-ddThh:mm:ssZ)")))
//    val limit = params.getAs[Int]("limit")
//    val offset = params.getAs[Int]("offset")
//
//    val dao = controller.daoFactory.newImagedMomentDAO()
//    dao.runTransaction(d => {
//      dao.streamByVideoReferenceUUID()
//    });
//
//    controller.findBetweenUpdatedDates(start, end, limit, offset)
//      .map(_.asJava)
//      .map(toJson)
//  }

}
