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

import org.slf4j.LoggerFactory

import java.sql.Connection
import scala.io.Source

object TestUtils {

  val Timeout        = duration.Duration(3, TimeUnit.SECONDS)
  val Digest         = MessageDigest.getInstance("SHA-512")
  private val random = Random

  private val log = LoggerFactory.getLogger(getClass)

  def runDdl(ddl: String, connection: Connection): Unit = {
    val statement = connection.createStatement();
    ddl
      .split(";")
      .foreach(sql => {
        log.warn(s"Running:\n$sql")
        statement.execute(sql)
      })
    statement.close()
  }

  def build(numIm: Int, numObs: Int, numAssoc: Int, numIr: Int): Seq[VideoSequence] = {
    val imDao   = daoFactory.newImagedMomentDAO()
    val obsDao  = daoFactory.newObservationDAO(imDao)
    val assDao  = daoFactory.newAssociationDAO(imDao)
    val dataDao = daoFactory.newCachedAncillaryDatumDAO(imDao)
    val irDao   = daoFactory.newImageReferenceDAO(imDao)
    for (imIdx <- 0 until numIm) {
      val im = imDao.new
    }

  }

}
