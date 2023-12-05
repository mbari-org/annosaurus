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

package org.mbari.vars.annosaurus.controllers

import org.mbari.vars.annotation.repository.jpa.BaseDAOSuite
import org.mbari.vars.annotation.dao.jpa.JPADAOFactory
import org.mbari.vars.annotation.controllers.AssociationController
import org.mbari.vars.annotation.controllers.BasicDAOFactory


trait AssociationControllerITSuite extends BaseDAOSuite {

    implicit val df: JPADAOFactory = daoFactory

    override def beforeAll(): Unit = daoFactory.beforeAll()
    override def afterAll(): Unit  = daoFactory.afterAll()

    lazy val controller = new AssociationController(daoFactory.asInstanceOf[BasicDAOFactory])
}
