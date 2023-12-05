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

import org.mbari.vars.annotation.dao.jpa.JPADAOFactory

import java.net.URL
import java.util.concurrent.TimeUnit
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.io.Source

trait ItDaoFactory extends JPADAOFactory {
  val TestProperties: Map[String, String] = Map(
    "eclipselink.logging.level.sql"                             -> "FINE",
    "eclipselink.logging.parameters"                            -> "true",
    "eclipselink.logging.level"                                 -> "INFO",
    "javax.persistence.schema-generation.scripts.action"        -> "none",
    "javax.persistence.schema-generation.database.action"       -> "none",
    "javax.persistence.schema-generation.scripts.create-target" -> "target/test-database-create.ddl"
  )

  def cleanup(): Unit = {

    import scala.concurrent.ExecutionContext.Implicits.global
    val dao = newImagedMomentDAO()

    val f = dao.runTransaction(_ => {
      val all = dao.findAll()
      all.foreach(dao.delete)
    })
    f.onComplete(_ => dao.close())
    Await.result(f, Duration(400, TimeUnit.SECONDS))

  }

  def testProps(): Map[String, String]
}
