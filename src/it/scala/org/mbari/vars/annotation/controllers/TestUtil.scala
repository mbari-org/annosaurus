package org.mbari.vars.annotation.controllers


import org.slf4j.LoggerFactory

import java.sql.Connection
import scala.io.Source

object TestUtil {

  private val log = LoggerFactory.getLogger(getClass)

  def runDdl(ddl: String, connection: Connection) {
    val statement = connection.createStatement();
    ddl.split(";").foreach(sql => {
      log.warn(s"Running:\n$sql")
      statement.execute(sql)
    })
//    statement.executeBatch()
    statement.close()
  }

}
