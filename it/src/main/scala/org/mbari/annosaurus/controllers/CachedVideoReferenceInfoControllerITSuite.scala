package org.mbari.annosaurus.controllers

import org.mbari.annosaurus.domain.CachedVideoReferenceInfo
import org.mbari.annosaurus.repository.jpa.{BaseDAOSuite, JPADAOFactory}

import scala.concurrent.ExecutionContext

trait CachedVideoReferenceInfoControllerITSuite extends BaseDAOSuite {

    given JPADAOFactory = daoFactory
    given ExecutionContext = ExecutionContext.global

    private lazy val controller = new CachedVideoReferenceInfoController(daoFactory)

    private def createOne(): CachedVideoReferenceInfo = {
        val vi = TestUtils.randomVideoReferenceInfo()
        exec(controller.create(vi.getVideoReferenceUuid, vi.getPlatformName, vi.getMissionId, Option(vi.getMissionContact)))
    }

    test("findByVideoReferenceUUID") {
        val existing = createOne()
        val obtained = exec(controller.findByVideoReferenceUUID(existing.videoReferenceUuid))
        assertEquals(obtained, Option(existing))
    }

    test("findByPlatformName") {
        val existing = createOne()
        val obtained = exec(controller.findByPlatformName(existing.platformName.get))
        assertEquals(obtained, Seq(existing))
    }

    test("findByMissionId") {
        val existing = createOne()
        val obtained = exec(controller.findByMissionId(existing.missionId.get))
        assertEquals(obtained, Seq(existing))
    }

    test("findByMissionContact") {
        val existing = createOne()
        val obtained = exec(controller.findByMissionContact(existing.missionContact.get))
        assertEquals(obtained, Seq(existing))
    }

    test("findAllMissionContacts") {
        val xs = (0 until 5).map(_ => createOne()).map(_.missionContact.get).toSet
        val obtained = exec(controller.findAllMissionContacts()).toSet
        for (x <- xs) assert(obtained.contains(x))
    }

    test("findAllPlatformNames") {
        val xs = (0 until 5).map(_ => createOne()).map(_.platformName.get).toSet
        val obtained = exec(controller.findAllPlatformNames()).toSet
        for (x <- xs) assert(obtained.contains(x))
    }

    test("findAllMissionIds") {
        val xs = (0 until 5).map(_ => createOne()).map(_.missionId.get).toSet
        val obtained = exec(controller.findAllMissionIds()).toSet
        for (x <- xs) assert(obtained.contains(x))
    }

    test("create") {
        val vi = TestUtils.randomVideoReferenceInfo()
        val obtained = exec(controller.create(vi.getVideoReferenceUuid, vi.getPlatformName, vi.getMissionId, Option(vi.getMissionContact)))
        val expected = CachedVideoReferenceInfo.from(vi)
            .copy(uuid = obtained.uuid, lastUpdated = obtained.lastUpdated)
        assertEquals(obtained, expected)
    }

    test("update") {
        val existing = createOne()
        val updated = existing.copy(missionContact = Option("new contact"), platformName = Some("new platform"), missionId = Some("new mission"))
        val opt = exec(controller.update(updated))
        opt match
            case None => fail("Expected an updated CachedVideoReferenceInfo")
            case Some(obtained) =>
                val obtained2 = obtained.copy(lastUpdated = updated.lastUpdated)
                assertEquals(obtained2, updated)
    }

    test("delete") {
        val existing = createOne()
        val ok = exec(controller.delete(existing.uuid))
        assert(ok)
        val opt = exec(controller.findByUUID(existing.uuid))
        assert(opt.isEmpty)
    }

    test("findAll") {
        val existing = createOne()
        val obtained = exec(controller.findAll()).toSeq
        assert(obtained.contains(existing))
    }

    test("findByUUID") {
        val existing = createOne()
        val obtained = exec(controller.findByUUID(existing.uuid))
        assertEquals(obtained, Option(existing))
    }

}
