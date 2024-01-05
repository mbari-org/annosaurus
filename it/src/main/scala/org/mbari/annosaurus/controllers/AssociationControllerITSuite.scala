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

import org.mbari.annosaurus.AssertUtils
import org.mbari.annosaurus.repository.jpa.BaseDAOSuite
import org.mbari.annosaurus.repository.jpa.JPADAOFactory

import scala.concurrent.ExecutionContext
import scala.jdk.CollectionConverters.*
import org.mbari.annosaurus.domain.Association
import junit.framework.Test
import org.mbari.annosaurus.repository.jpa.AssociationDAOImpl
import org.mbari.annosaurus.domain.ConceptAssociationRequest

trait AssociationControllerITSuite extends BaseDAOSuite {

    implicit val df: JPADAOFactory = daoFactory
//    override implicit val ec: ExecutionContext = ExecutionContext.global

    override def beforeAll(): Unit = daoFactory.beforeAll()
    override def afterAll(): Unit  = daoFactory.afterAll()

    lazy val controller = new AssociationController(daoFactory)

    test("delete") {
        val im                        = TestUtils.create(1, 1, 1).head
        val obs                       = im.getObservations.asScala.head
        val a                         = obs.getAssociations.iterator().next()
        val b                         = exec(controller.delete(a.getUuid()))
        assert(b)
        given dao: AssociationDAOImpl = daoFactory.newAssociationDAO()
        val opt                       = exec(dao.runTransaction(d => d.findByUUID(a.getUuid())))
        assert(opt.isEmpty)
    }

    test("findAll") {
        val im  = TestUtils.create(1, 1, 8, 0).head
        val obs = im.getObservations.asScala.head
        val xs  = obs.getAssociations.asScala
        val as  = exec(controller.findAll())
        for x <- xs
        do
            val a = as.find(_.uuid.get == x.getUuid)
            assert(a.isDefined)
            AssertUtils.assertSameAssociation(a.get.toEntity, x)
    }

    test("findByUUID") {
        val im  = TestUtils.create(1, 1, 1).head
        val obs = im.getObservations.asScala.head
        val a   = obs.getAssociations.iterator().next()
        val b   = exec(controller.findByUUID(a.getUuid()))
        assert(b.isDefined)
        AssertUtils.assertSameAssociation(b.get.toEntity, a)
    }

    test("create") {
        val x   = TestUtils.create(1, 1).head
        val obs = x.getObservations.asScala.head
        val a   = TestUtils.randomAssociation()
        val b   = exec(
            controller.create(
                obs.getUuid,
                a.getLinkName(),
                a.getToConcept(),
                a.getLinkValue(),
                a.getMimeType(),
                Option.empty
            )
        )
        assert(b.uuid.isDefined)
        a.setUuid(b.uuid.orNull)
        AssertUtils.assertSameAssociation(b.toEntity, a)
    }

    test("update") {
        val im    = TestUtils.create(1, 2, 1).head
        val xs    = im.getObservations.asScala
        val obs   = xs.head
        val other = xs.last
        val a     = obs.getAssociations.iterator().next()

        val c   = TestUtils.randomAssociation()
        val opt = exec(
            controller.update(
                a.getUuid(),
                Some(other.getUuid()), // Move to a different observation
                Some(c.getLinkName()),
                Some(c.getToConcept()),
                Some(c.getLinkValue()),
                Some(c.getMimeType())
            )
        )
        assert(opt.isDefined)
        val d   = opt.get
        assertEquals(d.uuid.orNull, a.getUuid())
        assertEquals(d.linkName, c.getLinkName())
        assertEquals(d.toConcept, c.getToConcept())
        assertEquals(d.linkValue, c.getLinkValue())
        assertEquals(d.mimeType.orNull, c.getMimeType())

        // TODO Make sure it moved to the other observation

    }

    test("bulkUpdate") {
        val x               = TestUtils.create(1, 1, 8, 0).head
        val associations    = x.getObservations.asScala.flatMap(_.getAssociations.asScala)
        associations.foreach(_.setLinkName("foobarbazbin"))
        val newAssociations = exec(controller.bulkUpdate(associations))
        val ax              = associations.toSeq.sortBy(_.getUuid)
        val bx              = newAssociations.toSeq.sortBy(_.uuid.get)
        ax.zip(bx).foreach(p => AssertUtils.assertSameAssociation(p._1, p._2.toEntity))

        // sanity check
        val dao = daoFactory.newAssociationDAO()
        bx.foreach { b =>
            val opt = exec(dao.runTransaction(d => d.findByUUID(b.uuid.get)))
            assert(opt.isDefined)
            // assertEquals(b, opt.map(Association.from(_)).get)
            AssertUtils.assertSameAssociation(b.toEntity, opt.get)
        }
    }

    test("bulkDelete") {
        val x            = TestUtils.create(1, 1, 8, 0).head
        val associations = x.getObservations.asScala.flatMap(_.getAssociations.asScala)
        val uuids        = associations.map(_.getUuid)
        exec(controller.bulkDelete(uuids))
        val xs           = exec(controller.findAll())
        for x <- associations
        do
            val a = xs.find(_.uuid == x.getUuid)
            assert(a.isEmpty)
    }

    test("findByLinkName") {
        val x   = TestUtils.create(1, 1, 1).head
        val obs = x.getObservations.asScala.head
        val a   = obs.getAssociations.iterator().next()
        val b   = exec(controller.findByLinkName(a.getLinkName()))
        assert(b.nonEmpty)
        AssertUtils.assertSameAssociation(b.head.toEntity, a)
    }

    test("findByLinkNameAndVideoReferenceUuid") {
        val x   = TestUtils.create(1, 1, 1).head
        val obs = x.getObservations.asScala.head
        val a   = obs.getAssociations.iterator().next()
        val b   = exec(
            controller.findByLinkNameAndVideoReferenceUuid(
                a.getLinkName(),
                x.getVideoReferenceUuid()
            )
        )
        assert(b.nonEmpty)
        AssertUtils.assertSameAssociation(b.head.toEntity, a)
    }

    test("findByLinkNameAndVideoReferenceUuidAndConcept") {
        val xs = TestUtils.create(2, 2, 2)

        // set all linknames to the same value
        val linkName = "yoyoyoyo"
        val ass      = xs.flatMap(_.getObservations().asScala).flatMap(_.getAssociations().asScala)
        for a <- ass
        do exec(controller.update(a.getUuid(), linkName = Some(linkName)))

        // use all 3 params
        val obs = xs.head.getObservations.asScala.head
        val a   = obs.getAssociations.iterator().next()
        val b   = exec(
            controller.findByLinkNameAndVideoReferenceUuidAndConcept(
                linkName,
                xs.head.getVideoReferenceUuid(),
                Some(obs.getConcept())
            )
        )
        assertEquals(b.size, 2)
        // AssertUtils.assertSameAssociation(b.head.toEntity, a)

        // use 2 params, omit concept
        val c = exec(
            controller.findByLinkNameAndVideoReferenceUuidAndConcept(
                linkName,
                xs.head.getVideoReferenceUuid()
            )
        )
        assertEquals(c.size, 8)

    }

    test("findByConceptAssociationRequest") {
        val im       = TestUtils.create(1, 1, 1) ++ TestUtils.create(1, 1, 1) ++ TestUtils.create(1, 1, 1)
        val vrus     = im.map(_.getVideoReferenceUuid())
        val ass      = im.head.getObservations().iterator().next().getAssociations().iterator().next()
        val linkName = ass.getLinkName()
        val cr       = new ConceptAssociationRequest(vrus, linkName)
        val response = exec(controller.findByConceptAssociationRequest(cr))
        assertEquals(response.associations.size, 1)
        val a        = response.associations.head
        AssertUtils.assertSameAssociation(a.toEntity, ass)

    }

    test("countByToConcept") {
        val im    = TestUtils.create(1, 1, 1).head
        val ass   = im.getObservations().iterator().next().getAssociations().iterator().next()
        val count = exec(controller.countByToConcept(ass.getToConcept()))
        assertEquals(count, 1L)
    }

    test("updateToConcept") {
        val im        = TestUtils.create(1, 1, 1).head
        val ass       = im.getObservations().iterator().next().getAssociations().iterator().next()
        val toConcept = "foobarbazbim"
        val count     = exec(controller.updateToConcept(ass.getToConcept(), toConcept))
        assertEquals(count, 1)
        val opt       = exec(controller.findByUUID(ass.getUuid()))
        assert(opt.isDefined)
        val ass0      = opt.get
        assertEquals(ass0.toConcept, toConcept)
    }
}
