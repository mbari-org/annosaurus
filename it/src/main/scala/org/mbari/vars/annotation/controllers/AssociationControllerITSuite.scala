package org.mbari.vars.annosaurus.controllers

trait AssociationControllerITSuite extends BaseDAOSuite {
  
    implicit val JPADAOFactory = daoFactory

    override def beforeAll(): Unit = daoFactory.beforeAll()
    override def afterAll(): Unit  = daoFactory.afterAll()

    lazy val controller = AssociationController(daoFactory)
}
