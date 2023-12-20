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

package org.mbari.annosaurus.controllers

import org.mbari.annosaurus.repository.IndexDAO
import java.time.Instant
import java.util.UUID

import scala.concurrent.{ExecutionContext, Future}
import org.mbari.annosaurus.repository.jpa.entity.ImagedMomentEntity
import org.mbari.annosaurus.repository.jpa.JPADAOFactory
import org.mbari.annosaurus.repository.jpa.entity.IndexEntity

/** @author
  *   Brian Schlining
  * @since 2019-02-08T11:00:00
  */
class IndexController(val daoFactory: JPADAOFactory)
    extends BaseController[IndexEntity, IndexDAO[IndexEntity]] {

    protected type IDDAO = IndexDAO[IndexEntity]

    override def newDAO(): IndexDAO[IndexEntity] = daoFactory.newIndexDAO()

    def findByVideoReferenceUUID(uuid: UUID, limit: Option[Int] = None, offset: Option[Int] = None)(
        implicit ec: ExecutionContext
    ): Future[Iterable[IndexEntity]] =
        exec(d => d.findByVideoReferenceUuid(uuid, limit, offset))

    /** Updates all recordedTimestamps thave have an elapsed time using the updated video
      * starttimestamp
      * @param videoReferenceUuid
      * @param newStartTimestamp
      * @param ec
      * @return
      */
    def updateRecordedTimestamps(videoReferenceUuid: UUID, newStartTimestamp: Instant)(implicit
        ec: ExecutionContext
    ): Future[Iterable[IndexEntity]] = {
        def fn(dao: IDDAO): Iterable[IndexEntity] = {
            dao
                .findByVideoReferenceUuid(videoReferenceUuid)
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

    def bulkUpdateRecordedTimestamps(
        imagedMoments: Iterable[IndexEntity]
    )(implicit ec: ExecutionContext): Future[Iterable[IndexEntity]] = {
        def fn(dao: IDDAO): Iterable[IndexEntity] = {
            (for (im <- imagedMoments) yield {
                dao
                    .findByUUID(im.uuid)
                    .map(i => {
                        Option(im.recordedDate).foreach(d => i.recordedDate = d)
                        i
                    })
            }).flatten
        }
        exec(fn)
    }

}
