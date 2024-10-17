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

import org.mbari.annosaurus.repository.CachedVideoReferenceInfoDAO
import java.util.UUID

import scala.concurrent.{ExecutionContext, Future}
import org.mbari.annosaurus.repository.jpa.entity.CachedVideoReferenceInfoEntity
import org.mbari.annosaurus.repository.jpa.JPADAOFactory
import org.mbari.annosaurus.domain.CachedVideoReferenceInfo
import org.checkerframework.checker.units.qual.C

/**
 * @author
 *   Brian Schlining
 * @since 2016-09-14T10:50:00
 */
class CachedVideoReferenceInfoController(val daoFactory: JPADAOFactory)
    extends BaseController[CachedVideoReferenceInfoEntity, CachedVideoReferenceInfoDAO[
        CachedVideoReferenceInfoEntity
    ], CachedVideoReferenceInfo]:

    protected type VRDAO = CachedVideoReferenceInfoDAO[CachedVideoReferenceInfoEntity]

    override def newDAO(): CachedVideoReferenceInfoDAO[CachedVideoReferenceInfoEntity] =
        daoFactory.newCachedVideoReferenceInfoDAO()

    override def transform(a: CachedVideoReferenceInfoEntity): CachedVideoReferenceInfo =
        CachedVideoReferenceInfo.from(a, true)

    //  def findAll(limit: Int, offset: Int)(implicit ec: ExecutionContext): Future[Iterable[CachedVideoReferenceInfo]] =
    //    exec(d => d.findAll(limit, offset))

    def findByVideoReferenceUUID(
        uuid: UUID
    )(implicit ec: ExecutionContext): Future[Option[CachedVideoReferenceInfo]] =
        def fn(dao: VRDAO): Option[CachedVideoReferenceInfo] =
            dao.findByVideoReferenceUUID(uuid).map(transform)
        exec(fn)

    def findByPlatformName(
        name: String
    )(implicit ec: ExecutionContext): Future[Iterable[CachedVideoReferenceInfo]] =
        def fn(dao: VRDAO): Iterable[CachedVideoReferenceInfo] =
            dao.findByPlatformName(name).map(transform)
        exec(fn)

    def findByMissionId(
        id: String
    )(implicit ec: ExecutionContext): Future[Iterable[CachedVideoReferenceInfo]] =
        def fn(dao: VRDAO): Iterable[CachedVideoReferenceInfo] =
            dao.findByMissionID(id).map(transform)
        exec(fn)

    def findByMissionContact(
        contact: String
    )(implicit ec: ExecutionContext): Future[Iterable[CachedVideoReferenceInfo]] =
        def fn(dao: VRDAO): Iterable[CachedVideoReferenceInfo] =
            dao.findByMissionContact(contact).map(transform)
        exec(fn)

    def findAllMissionContacts()(implicit ec: ExecutionContext): Future[Iterable[String]] =
        def fn(dao: VRDAO): Iterable[String] = dao.findAllMissionContacts()
        exec(fn)

    def findAllPlatformNames()(implicit ec: ExecutionContext): Future[Iterable[String]] =
        def fn(dao: VRDAO): Iterable[String] = dao.findAllPlatformNames()
        exec(fn)

    def findAllMissionIds()(implicit ec: ExecutionContext): Future[Iterable[String]] =
        def fn(dao: VRDAO): Iterable[String] = dao.findAllMissionIDs()
        exec(fn)

    def findAllVideoReferenceUUIDs()(implicit ec: ExecutionContext): Future[Iterable[UUID]] =
        def fn(dao: VRDAO): Iterable[UUID] = dao.findAllVideoReferenceUUIDs()
        exec(fn)

    def create(
        videoReferenceUUID: UUID,
        platformName: String,
        missionID: String,
        missionContact: Option[String] = None
    )(implicit ec: ExecutionContext): Future[CachedVideoReferenceInfo] =

        def fn(dao: VRDAO): CachedVideoReferenceInfo =
            val v = new CachedVideoReferenceInfoEntity
            v.setVideoReferenceUuid(videoReferenceUUID)
            v.setPlatformName(platformName)
            v.setMissionId(missionID)
            missionContact.foreach(v.setMissionContact)
            dao.create(v)
            transform(v)
        exec(fn)

    def update(
        info: CachedVideoReferenceInfo
    )(using ec: ExecutionContext): Future[Option[CachedVideoReferenceInfo]] =
        update(
            info.uuid,
            Option(info.videoReferenceUuid),
            info.platformName,
            info.missionId,
            info.missionContact
        )

    def update(
        uuid: UUID,
        videoReferenceUUID: Option[UUID] = None,
        platformName: Option[String] = None,
        missionID: Option[String] = None,
        missionContact: Option[String] = None
    )(implicit ec: ExecutionContext): Future[Option[CachedVideoReferenceInfo]] =

        def fn(dao: VRDAO): Option[CachedVideoReferenceInfo] = dao.findByUUID(uuid) match
            case None    => None
            case Some(v) =>
                videoReferenceUUID.foreach(v.setVideoReferenceUuid)
                platformName.foreach(v.setPlatformName)
                missionID.foreach(v.setMissionId)
                missionContact.foreach(v.setMissionContact)
                Some(transform(v))
        exec(fn)
