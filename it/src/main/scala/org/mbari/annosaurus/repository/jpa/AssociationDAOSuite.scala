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

package org.mbari.annosaurus.repository.jpa

import org.mbari.annosaurus.controllers.TestUtils
import scala.jdk.CollectionConverters.*
import org.mbari.annosaurus.AssertUtils
import org.mbari.annosaurus.domain.ConceptAssociationRequest

trait AssociationDAOSuite extends BaseDAOSuite {

    given JPADAOFactory = daoFactory

    test("findByLinkName") {
        val xs                        = TestUtils.create(1, 2, 2)
        val i                         = xs.head
        val o                         = i.getObservations.asScala.head
        val a                         = o.getAssociations.asScala.head
        given dao: AssociationDAOImpl = daoFactory.newAssociationDAO()
        val ys                        = run(() => dao.findByLinkName(a.getLinkName))
        assert(ys.size == 1)
        AssertUtils.assertSameAssociation(a, ys.head)
    }

    test("findByLinkNameAndVideoReferenceUUID") {
        val xs                        = TestUtils.create(1, 2, 2)
        val i                         = xs.head
        val o                         = i.getObservations.asScala.head
        val a                         = o.getAssociations.asScala.head
        given dao: AssociationDAOImpl = daoFactory.newAssociationDAO()
        val ys                        = run(() =>
            dao.findByLinkNameAndVideoReferenceUUID(a.getLinkName, i.getVideoReferenceUuid)
        )
        assert(ys.size == 1)
        AssertUtils.assertSameAssociation(a, ys.head)
    }

    test("findByLinkNameAndVideoReferenceUUIDAndConcept") {
        val xs                        = TestUtils.create(1, 2, 2)
        val i                         = xs.head
        val o                         = i.getObservations.asScala.head
        val a                         = o.getAssociations.asScala.head
        given dao: AssociationDAOImpl = daoFactory.newAssociationDAO()
        val ys                        = run(() =>
            dao.findByLinkNameAndVideoReferenceUUIDAndConcept(
                a.getLinkName,
                i.getVideoReferenceUuid
            )
        )
        assert(ys.size == 1)
        AssertUtils.assertSameAssociation(a, ys.head)

        val zs = run(() =>
            dao.findByLinkNameAndVideoReferenceUUIDAndConcept(
                a.getLinkName,
                i.getVideoReferenceUuid,
                Some(o.getConcept())
            )
        )
        assert(zs.size == 1)
        AssertUtils.assertSameAssociation(a, zs.head)
    }

    test("findByConceptAssociationRequest") {
        val xs                        = TestUtils.create(1, 2, 2)
        val i                         = xs.head
        val o                         = i.getObservations.asScala.head
        val a                         = o.getAssociations.asScala.head
        given dao: AssociationDAOImpl = daoFactory.newAssociationDAO()
        val r                         = ConceptAssociationRequest(Seq(i.getVideoReferenceUuid()), a.getLinkName())
        val ys                        = run(() => dao.findByConceptAssociationRequest(r))
        assert(ys.size == 1)
        val c                         = ys.head
        assertEquals(a.getLinkName(), c.linkName)
        assertEquals(a.getToConcept(), c.toConcept)
        assertEquals(a.getLinkValue(), c.linkValue)
        assertEquals(o.getConcept(), c.concept)
        assertEquals(a.getMimeType(), c.mimeType)
        assertEquals(i.getVideoReferenceUuid(), c.videoReferenceUuid)
        assertEquals(a.getUuid(), c.uuid)

    }

    test("findAll") {
        val xs                        = TestUtils.create(1, 2, 2)
        given dao: AssociationDAOImpl = daoFactory.newAssociationDAO()
        val ys                        = run(() => dao.findAll())
        assert(ys.size >= 2)
    }

    test("countByToConcept") {
        val xs                        = TestUtils.create(1, 2, 2)
        val i                         = xs.head
        val o                         = i.getObservations.asScala.head
        val a                         = o.getAssociations.asScala.head
        given dao: AssociationDAOImpl = daoFactory.newAssociationDAO()

        val ys = run(() => dao.countByToConcept(a.getToConcept()))
        assert(ys == 1)
    }

    test("updateToConcept") {
        val xs                        = TestUtils.create(1, 1, 1)
        val i                         = xs.head
        val o                         = i.getObservations.asScala.head
        val a                         = o.getAssociations.asScala.head
        given dao: AssociationDAOImpl = daoFactory.newAssociationDAO()

        val ys = run(() => dao.updateToConcept(a.getToConcept(), "newConcept"))
        assert(ys == 1)

        val ws = run(() => dao.findByUUID(a.getUuid()))
        assert(ws.isDefined)
        a.setToConcept("newConcept") // update the original object
        AssertUtils.assertSameAssociation(ws.get, a)
    }

    test("newPersistentObject") {}
    test("create") {

        val xs = TestUtils.create(1, 1)
        val i  = xs.head
        val o  = i.getObservations.asScala.head
        val a  = TestUtils.randomAssociation()
        o.addAssociation(a)

        // We DO NOT use AssocationDAO.create ever.  Here's the
        // correct way to create an association
        given dao: AssociationDAOImpl = daoFactory.newAssociationDAO()
        val obsDao                    = daoFactory.newObservationDAO(dao)
        run(() => {
            obsDao
                .findByUUID(o.getUuid())
                .foreach(obs => {
                    obs.addAssociation(a)
                })
        })
        val ys                        = run(() => dao.findByUUID(a.getUuid()))
        assert(ys.isDefined)
        AssertUtils.assertSameAssociation(ys.get, a)
    }
    test("update") {
        val xs                        = TestUtils.create(1, 1, 1)
        val i                         = xs.head
        val o                         = i.getObservations.asScala.head
        val a                         = o.getAssociations().asScala.head
        given dao: AssociationDAOImpl = daoFactory.newAssociationDAO()
        a.setToConcept("foo")
        a.setLinkName("bar")
        a.setLinkValue("baz")
        val b                         = run(() => dao.update(a))
        AssertUtils.assertSameAssociation(a, b)

    }
    test("delete") {
        val xs                        = TestUtils.create(1, 1, 1)
        val i                         = xs.head
        val o                         = i.getObservations.asScala.head
        val a                         = o.getAssociations().asScala.head
        given dao: AssociationDAOImpl = daoFactory.newAssociationDAO()
        run(() =>
            dao.findByUUID(a.getUuid()) match {
                case Some(x) => dao.delete(x)
                case None    => fail("Could not find association")
            }
        )
        val ys                        = run(() => dao.findByUUID(a.getUuid()))
        assert(ys.isEmpty)
    }

    test("deleteByUUID") {
        val xs                        = TestUtils.create(1, 1, 1)
        val i                         = xs.head
        val o                         = i.getObservations.asScala.head
        val a                         = o.getAssociations().asScala.head
        given dao: AssociationDAOImpl = daoFactory.newAssociationDAO()
        run(() => dao.deleteByUUID(a.getUuid()))
        val ys                        = run(() => dao.findByUUID(a.getUuid()))
        assert(ys.isEmpty)
    }
    test("findByUUID") {
        val xs                        = TestUtils.create(1, 1, 1)
        val i                         = xs.head
        val o                         = i.getObservations.asScala.head
        val a                         = o.getAssociations().asScala.head
        given dao: AssociationDAOImpl = daoFactory.newAssociationDAO()
        val ys                        = run(() => dao.findByUUID(a.getUuid()))
        assert(ys.isDefined)
        AssertUtils.assertSameAssociation(ys.get, a)
    }

}
