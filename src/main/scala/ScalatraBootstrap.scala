import javax.servlet.ServletContext

import org.scalatra.LifeCycle
import org.scalatra.swagger.{ ApiInfo, Swagger }
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext

/**
  *
  *
  * @author Brian Schlining
  * @since 2016-05-20T14:41:00
  */
class ScalatraBootstrap extends LifeCycle {

  private[this] val log = LoggerFactory.getLogger(getClass)

  val apiInfo = ApiInfo(
    """video-asset-manager""",
    """Video Asset Manager - Server""",
    """http://localhost:8080/api-docs""",
    """brian@mbari.org""",
    """MIT""",
    """http://opensource.org/licenses/MIT""")

  implicit val swagger = new Swagger("1.2", "1.0.0", apiInfo)

  override def init(context: ServletContext): Unit = {

    println("STARTING UP NOW")

    implicit val executionContext = ExecutionContext.global

  }

}
