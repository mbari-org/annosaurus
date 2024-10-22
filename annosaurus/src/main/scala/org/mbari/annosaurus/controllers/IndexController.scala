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

import org.mbari.annosaurus.domain.{Index, IndexUpdate}
import org.mbari.annosaurus.repository.IndexDAO
import org.mbari.annosaurus.repository.jpa.JPADAOFactory
import org.mbari.annosaurus.repository.jpa.entity.IndexEntity

import java.time.Instant
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

/**
 * @author
 *   Brian Schlining
 * @since 2019-02-08T11:00:00
 */
class IndexController(val daoFactory: JPADAOFactory) extends BaseController[IndexEntity, IndexDAO[IndexEntity], Index]:

    protected type IDDAO = IndexDAO[IndexEntity]

    override def newDAO(): IndexDAO[IndexEntity] = daoFactory.newIndexDAO()

    override def transform(a: IndexEntity): Index = Index.from(a, true)

    def findByVideoReferenceUUID(uuid: UUID, limit: Option[Int] = None, offset: Option[Int] = None)(implicit
        ec: ExecutionContext
    ): Future[Iterable[Index]] =
        exec(d => d.findByVideoReferenceUuid(uuid, limit, offset).map(transform))

    /**
     * Updates all recordedTimestamps thave have an elapsed time using the updated video starttimestamp
     * @param videoReferenceUuid
     * @param newStartTimestamp
     * @param ec
     * @return
     */
    def updateRecordedTimestamps(videoReferenceUuid: UUID, newStartTimestamp: Instant)(implicit
        ec: ExecutionContext
    ): Future[Iterable[Index]] =
        def fn(dao: IDDAO): Iterable[Index] =
            dao
                .findByVideoReferenceUuid(videoReferenceUuid)
                .map(im =>
                    if im.getElapsedTime != null then
                        val newRecordedDate = newStartTimestamp.plus(im.getElapsedTime)
                        if newRecordedDate != im.getRecordedTimestamp then im.setRecordedTimestamp(newRecordedDate)
                    transform(im)
                )
        exec(fn)

    def bulkUpdateRecordedTimestamps(
        imagedMoments: Iterable[IndexUpdate]
    )(implicit ec: ExecutionContext): Future[Iterable[Index]] =
        def fn(dao: IDDAO): Iterable[Index] =
            for
                im <- imagedMoments
                rt <- im.recordedTimestamp
                i  <- dao.findByUUID(im.uuid)
            yield
                i.setRecordedTimestamp(rt)
                transform(i)
        exec(fn)
