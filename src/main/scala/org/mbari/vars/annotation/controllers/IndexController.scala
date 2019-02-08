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

package org.mbari.vars.annotation.controllers

import java.time.Instant
import java.util.UUID

import org.mbari.vars.annotation.dao.IndexDAO
import org.mbari.vars.annotation.model.ImagedMoment

import scala.concurrent.{ ExecutionContext, Future }

/**
 * @author Brian Schlining
 * @since 2019-02-08T11:00:00
 */
class IndexController(val daoFactory: BasicDAOFactory)
  extends BaseController[ImagedMoment, IndexDAO[ImagedMoment]] {

  protected type IDDAO = IndexDAO[ImagedMoment]

  override def newDAO(): IndexDAO[ImagedMoment] = daoFactory.newIndexDAO()

  def findByVideoReferenceUUID(uuid: UUID, limit: Option[Int] = None, offset: Option[Int] = None)(implicit ec: ExecutionContext): Future[Iterable[ImagedMoment]] =
    exec(d => d.findByVideoReferenceUuid(uuid, limit, offset))

  /**
   * Updates all recordedTimestamps thave have an elapsed time using
   * the updated video starttimestamp
   * @param videoReferenceUuid
   * @param newStartTimestamp
   * @param ec
   * @return
   */
  def updateRecordedTimestamps(videoReferenceUuid: UUID, newStartTimestamp: Instant)(implicit ec: ExecutionContext): Future[Iterable[ImagedMoment]] = {
    def fn(dao: IDDAO): Iterable[ImagedMoment] = {
      dao.findByVideoReferenceUuid(videoReferenceUuid)
        .map(im => {
          if (im.elapsedTime != null) {
            val newRecordedDate = newStartTimestamp.plus(im.elapsedTime)
            if (newRecordedDate != im.recordedDate) {
              im.recordedDate = newRecordedDate
            }
          }
          im
        })
    }
    exec(fn)
  }

  def bulkUpdateRecordedTimestamps(imagedMoments: Iterable[ImagedMoment])(implicit ec: ExecutionContext): Future[Iterable[ImagedMoment]] = {
    def fn(dao: IDDAO): Iterable[ImagedMoment] = {
      (for (im <- imagedMoments) yield {
        dao.findByUUID(im.uuid)
          .map(i => {
            Option(im.recordedDate).foreach(d => i.recordedDate = d)
            i
          })
      }).flatten
    }
    exec(fn)
  }

}
