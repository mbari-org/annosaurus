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

package org.mbari.vars.annotation.api

import java.net.{URI, URL}
import java.time.format.DateTimeFormatter
import java.time.{Duration, Instant}
import java.util.UUID
import java.util.concurrent.TimeUnit

import jakarta.servlet.http.HttpServletResponse
import org.mbari.annosaurus.Constants
import org.mbari.vcr4j.time.Timecode
import org.scalatra.util.conversion.TypeConverter
import org.scalatra.{FutureSupport, ScalatraServlet}
import org.slf4j.LoggerFactory

import scala.concurrent.duration.{Duration => SDuration}
import scala.concurrent.{Await, Future}
import scala.util.Try
import java.time

/**
  * All Api classes should mixin this trait. It defines the common traits used by all implementations
  * as well implicits need for type conversions.
  *
  * @author Brian Schlining
  * @since 2016-05-23T13:32:00
  */
abstract class APIStack extends ScalatraServlet with ApiAuthenticationSupport with FutureSupport {

  implicit def toScalaDuration(duration: Duration): SDuration =
    SDuration(duration.toNanos, TimeUnit.NANOSECONDS)

  protected[this] val log                  = LoggerFactory.getLogger(getClass)
  protected[this] val timeFormatter        = DateTimeFormatter.ISO_DATE_TIME
  protected[this] val compactTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssX")
  protected[this] val compactTimeFormatter1 = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss.SSSX")
  protected[this] val compactTimeFormatter2 = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss.SSSSSSX")
  protected[this] val defaultLimit         = 50

  //protected[this] implicit val jsonFormats: Formats = DefaultFormats ++ JavaTypesSerializers.all

  implicit val stringToUUID: TypeConverter[String, UUID] = new TypeConverter[String, UUID] {
    override def apply(s: String): Option[UUID] = Try(UUID.fromString(s)).toOption
  }

  // implicit protected val stringToInstant = new TypeConverter[String, Instant] {
  //   override def apply(s: String): Option[Instant] = {
  //     val try1 = Try(Instant.from(compactTimeFormatter.parse(s))).toOption
  //     try1 match {
  //       case Some(t) => try1
  //       case None    => Try(Instant.from(timeFormatter.parse(s))).toOption
  //     }
  //   }
  // }

  implicit val stringToInstant: TypeConverter[String, Instant] = new TypeConverter[String, Instant] {
    override def apply(s: String): Option[Instant] = {
      val try1 = Try(Instant.from(compactTimeFormatter.parse(s))).toOption
      val try2 = try1 match {
        case Some(_) => try1
        case None => Try(Instant.from(timeFormatter.parse(s))).toOption
      }
      val try3 = try2 match {
        case Some(_) => try2
        case None => Try(Instant.from(compactTimeFormatter1.parse(s))).toOption
      }
      val try4 = try3 match {
        case Some(_) => try3
        case None => Try(Instant.from(compactTimeFormatter2.parse(s))).toOption
      }
      try4
    }
  }

  implicit val stringToDuration: TypeConverter[String, time.Duration] = new TypeConverter[String, Duration] {
    override def apply(s: String): Option[Duration] = Try(Duration.ofMillis(s.toLong)).toOption
  }

  implicit val stringToURI: TypeConverter[String, URI] = new TypeConverter[String, URI] {
    override def apply(s: String): Option[URI] = Try(URI.create(s)).toOption
  }

  implicit val stringToURL: TypeConverter[String, URL] = new TypeConverter[String, URL] {
    override def apply(s: String): Option[URL] = Try(URI.create(s).toURL()).toOption
  }

  implicit val stringToTimecode: TypeConverter[String, Timecode] = new TypeConverter[String, Timecode] {
    override def apply(s: String): Option[Timecode] = Try(new Timecode(s)).toOption
  }

  def toJson(obj: Any): String                      = Constants.GSON.toJson(obj)
  def fromJson[T](json: String, classOfT: Class[T]) = Constants.GSON.fromJson(json, classOfT)

  /**
    * "Stream" the iterable data as a chunked response.
    * @param response The original response
    * @param items The items to stream back
    * @tparam T The type of each item in items
    */
  def sendChunkedResponse[T](response: HttpServletResponse, items: Iterable[T]): Unit = {

    response.setHeader("Transfer-Encoding", "chunked")
    response.setStatus(200)
    val out = response.getWriter
    out.write("[")
    def write(item: T, remaining: Iterable[T]): Unit = {
      out.write(toJson(item))
      out.flush()
      if (remaining.nonEmpty) {
        out.write(",")
        write(remaining.head, remaining.tail)
      }
      else out.write("]")
    }
    write(items.head, items.tail)
    out.flush()

  }

  /**
    * Pages data and sends each page back as a chunked response to the server
    * @param response The response object
    * @param start The starting index of the data to stream (same as offset)
    * @param end The ending index of the data to stream (could be offset + limit)
    * @param pageSize The number of items in each chunked page
    * @param fn A function that takes (limit, offset) and fetches a iterable
    *           of items (as a future). This function is fetching a page of
    *           data from the database.
    * @param timeout The timeout for a chunk.
    * @tparam T The type of items returned from the database.
    */
  def autoPage[T](
      response: HttpServletResponse,
      start: Int,
      end: Int,
      pageSize: Int,
      fn: (Int, Int) => Future[Iterable[T]],
      timeout: Duration = Duration.ofSeconds(20)
  ): Unit = {

    log.debug(s"Executing: autoPage(..., $start, $end, $pageSize, ..., ...)")

    response.setHeader("Transfer-Encoding", "chunked")
    response.setStatus(200)
    val out = response.getWriter

    def processChunk(limit: Int, offset: Int, prependComma: Boolean): Unit = {
      val data = Await.result(fn(limit, offset), timeout)
      if (data.nonEmpty) {
        if (prependComma) out.write(",")
        val s = data.map(toJson).mkString(",")
        out.write(s)
        out.flush()
      }
    }

    out.write("[")
    for (j <- start to end by pageSize) {
      val prependComma = j > start
      val limit        = if (j + pageSize > end) end - j else pageSize
      log.debug(s"Executing: autoPage::processChunk($limit, $j, $prependComma)")
      if (limit > 0) {
        processChunk(limit, j, prependComma)
      }
    }
    out.write("]")
    out.flush()

  }

}
