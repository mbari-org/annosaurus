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

trait AssociationControllerITSuite extends BaseDAOSuite {

    implicit val df: JPADAOFactory = daoFactory
//    override implicit val ec: ExecutionContext = ExecutionContext.global

    override def beforeAll(): Unit = daoFactory.beforeAll()
    override def afterAll(): Unit  = daoFactory.afterAll()

    lazy val controller = new AssociationController(daoFactory)

    test("delete") {}

    test("findAll") {}

    test("findByUUID") {}

    test("create") {
        val x = TestUtils.create(1, 1).head
        val obs = x.getObservations.asScala.head
        val a = TestUtils.randomAssociation()
        val b = exec(
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
        val im = TestUtils.create(1, 2, 1).head
        val xs = im.getObservations.asScala
        val obs = xs.head
        val other = xs.last
        val a = obs.getAssociations.iterator().next()

        val c = TestUtils.randomAssociation()
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
        val d = opt.get
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
}
