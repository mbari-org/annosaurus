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

import java.util.concurrent.Executors

import javax.servlet.ServletContext
import org.mbari.vars.annotation.api.v1._
import org.mbari.vars.annotation.api.v2._
import org.mbari.vars.annotation.controllers._
import org.mbari.vars.annotation.dao.jpa.JPADAOFactory
import org.scalatra.LifeCycle
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
import org.mbari.vars.annotation.api.v1.HealthApi
import org.mbari.vars.annotation.AppConfig

/**
  *
  *
  * @author Brian Schlining
  * @since 2016-05-20T14:41:00
  */
class ScalatraBootstrap extends LifeCycle {

  private[this] val log = LoggerFactory.getLogger(getClass)

  override def init(context: ServletContext): Unit = {

    LoggerFactory.getLogger(getClass).info(s"Mounting ${AppConfig.Name} Servlets")
    // Optional because * is the default
    context.setInitParameter("org.scalatra.cors.allowedOrigins", "*")
    // Disables cookies, but required because browsers will not allow passing credentials to wildcard domains
    context.setInitParameter("org.scalatra.cors.allowCredentials", "false")

    implicit val executionContext: ExecutionContext =
      ExecutionContext.fromExecutor(Executors.newWorkStealingPool())

    val daoFactory: BasicDAOFactory = JPADAOFactory.asInstanceOf[BasicDAOFactory]
    val ancillaryDatumController    = new CachedAncillaryDatumController(daoFactory)
    val annotationController        = new AnnotationController(daoFactory)
    val associationController       = new AssociationController(daoFactory)
    val imageController             = new ImageController(daoFactory)
    val imagedMomentController      = new ImagedMomentController(daoFactory)
    val imageReferenceController    = new ImageReferenceController(daoFactory)
    val indexController             = new IndexController(daoFactory)
    val observationController       = new ObservationController(daoFactory)
    val videoReferenceController    = new CachedVideoReferenceInfoController(daoFactory)

    val ancillaryDatumV1Api = new CachedAncillaryDatumV1Api(ancillaryDatumController)
    val annotationV1Api     = new AnnotationV1Api(annotationController)
    val associationV1Api    = new AssociationV1Api(associationController)
    val authorizationV1Api  = new AuthorizationV1Api
    val imagedMomentV1Api   = new ImagedMomentV1Api(imagedMomentController)
    val imageReferenceV1Api = new ImageReferenceV1Api(imageReferenceController)
    val imageV1Api          = new ImageV1Api(imageController)
    val indexV1Api          = new IndexV1Api(indexController)
    val observationV1Api    = new ObservationV1Api(observationController)
    val videoReferenceV1Api = new CachedVideoReferenceInfoV1Api(videoReferenceController)

    val annotationV2Api   = new AnnotationV2Api(annotationController)
    val imagedMomentV2Api = new ImagedMomentV2Api(imagedMomentController)

    val fastAnnotationV1Api = new FastAnnotationV1Api(JPADAOFactory)
    val analysisV1Api       = new AnalysisV1Api(JPADAOFactory)

    context.mount(ancillaryDatumV1Api, "/v1/ancillarydata")
    context.mount(analysisV1Api, "/v1/analysis")
    context.mount(annotationV1Api, "/v1/annotations")
    context.mount(associationV1Api, "/v1/associations")
    context.mount(authorizationV1Api, "/v1/auth")
    context.mount(fastAnnotationV1Api, "/v1/fast")
    context.mount(imagedMomentV1Api, "/v1/imagedmoments")
    context.mount(imageReferenceV1Api, "/v1/imagereferences")
    context.mount(imageV1Api, "/v1/images")
    context.mount(indexV1Api, "/v1/index")
    context.mount(new HealthApi, "/v1/health")
    context.mount(observationV1Api, "/v1/observations")
    context.mount(videoReferenceV1Api, "/v1/videoreferences")

    context.mount(annotationV2Api, "/v2/annotations")
    context.mount(imagedMomentV2Api, "/v2/imagedmoments")

  }

}
