import javax.servlet.ServletContext

import org.mbari.vars.annotation.api._
import org.mbari.vars.annotation.controllers._
import org.mbari.vars.annotation.dao.jpa.JPADAOFactory
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
    """video-annotation-service""",
    """Video Annotations - Server""",
    """http://localhost:8080/api-docs""",
    """brian@mbari.org""",
    """MIT""",
    """http://opensource.org/licenses/MIT"""
  )

  implicit val swagger = new Swagger("1.2", "1.0.0", apiInfo)

  override def init(context: ServletContext): Unit = {

    log.info("STARTING UP NOW")

    implicit val executionContext = ExecutionContext.global

    val daoFactory: BasicDAOFactory = JPADAOFactory.asInstanceOf[BasicDAOFactory]
    val ancillaryDatumController = new CachedAncillaryDatumController(daoFactory)
    val annotationController = new AnnotationController(daoFactory)
    val associationController = new AssociationController(daoFactory)
    val imageController = new ImageController(daoFactory)
    val imagedMomentController = new ImagedMomentController(daoFactory)
    val imageReferenceController = new ImageReferenceController(daoFactory)
    val observationController = new ObservationController(daoFactory)
    val videoReferenceController = new CachedVideoReferenceInfoController(daoFactory)

    val ancillaryDatumV1Api = new CachedAncillaryDatumV1Api(ancillaryDatumController)
    val annotationV1Api = new AnnotationV1Api(annotationController)
    val associationV1Api = new AssociationV1Api(associationController)
    val authorizationV1Api = new AuthorizationV1Api
    val imagedMomentV1Api = new ImagedMomentV1Api(imagedMomentController)
    val imageReferenceV1Api = new ImageReferenceV1Api(imageReferenceController)
    val imageV1Api = new ImageV1Api(imageController)
    val observationV1Api = new ObservationV1Api(observationController)
    val videoReferenceV1Api = new CachedVideoReferenceInfoV1Api(videoReferenceController)

    context.mount(ancillaryDatumV1Api, "/v1/ancillarydata")
    context.mount(annotationV1Api, "/v1/annotations")
    context.mount(associationV1Api, "/v1/associations")
    context.mount(authorizationV1Api, "/v1/auth")
    context.mount(imagedMomentV1Api, "/v1/imagedmoments")
    context.mount(imageReferenceV1Api, "/v1/imagereferences")
    context.mount(imageV1Api, "/v1/images")
    context.mount(observationV1Api, "/v1/observations")
    context.mount(videoReferenceV1Api, "/v1/videoreferences")

    context.mount(new AnnotationSwagger, "/api-docs")

  }

}
