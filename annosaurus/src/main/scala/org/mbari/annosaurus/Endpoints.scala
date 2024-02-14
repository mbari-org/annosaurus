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

package org.mbari.annosaurus

import org.mbari.annosaurus.controllers.*
import org.mbari.annosaurus.endpoints.*
import org.mbari.annosaurus.etc.jwt.JwtService
import org.mbari.annosaurus.repository.jdbc.{AnalysisRepository, JdbcRepository}
import org.mbari.annosaurus.repository.jpa.JPADAOFactory
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.metrics.prometheus.PrometheusMetrics
import sttp.tapir.swagger.bundle.SwaggerInterpreter

import scala.concurrent.{ExecutionContext, Future}

object Endpoints {

    // --------------------------------
    given ExecutionContext = ExecutionContext.global
    given JwtService       = AppConfig.DefaultJwtService
    val daoFactory         = JPADAOFactory

    // --------------------------------
    val annotationController               = new AnnotationController(daoFactory)
    val associationController              = new AssociationController(daoFactory)
    val cachedAncillaryDatumController     = new CachedAncillaryDatumController(daoFactory)
    val cachedVideoReferenceInfoController = new CachedVideoReferenceInfoController(daoFactory)
    val imageController                    = new ImageController(daoFactory)
    val imagedMomentController             = new ImagedMomentController(daoFactory)
    val imageReferenceController           = new ImageReferenceController(daoFactory)
    val indexController                    = new IndexController(daoFactory)
    val observationController              = new ObservationController(daoFactory)

    // --------------------------------
    val analysisRepository = new AnalysisRepository(daoFactory.entityManagerFactory)
    val jdbcRepository     = new JdbcRepository(daoFactory.entityManagerFactory)

    // --------------------------------
    val analysisEndpoints                 = new AnalysisEndpoints(analysisRepository)
    val annotationEndpoints               = new AnnotationEndpoints(annotationController)
    val associationEndpoints              = new AssociationEndpoints(associationController)
    val authorizationEndpoints            = new AuthorizationEndpoints()
    val cachedAncillaryDatumEndpoints     = new CachedAncillaryDatumEndpoints(
        cachedAncillaryDatumController
    )
    val cachedVideoReferenceInfoEndpoints = new CachedVideoReferenceInfoEndpoints(
        cachedVideoReferenceInfoController
    )
    val fastAnnotationEndpoints           = new FastAnnotationEndpoints(jdbcRepository)
    val healthEndpoints                   = new HealthEndpoints()
    val imagedMomentEndpoints             = new ImagedMomentEndpoints(imagedMomentController)
    val imageEndpoints                    = new ImageEndpoints(imageController)
    val imageReferenceEndpoints           = new ImageReferenceEndpoints(imageReferenceController)
    val indexEndpoints                    = new IndexEndpoints(indexController)
    val observationEndpoints              = new ObservationEndpoints(observationController)

    // --------------------------------
    val apiEndpoints = analysisEndpoints.allImpl ++
        annotationEndpoints.allImpl ++
        associationEndpoints.allImpl ++
        authorizationEndpoints.allImpl ++
        cachedAncillaryDatumEndpoints.allImpl ++
        cachedVideoReferenceInfoEndpoints.allImpl ++
        fastAnnotationEndpoints.allImpl ++
        healthEndpoints.allImpl ++
        imagedMomentEndpoints.allImpl ++
        imageEndpoints.allImpl ++
        imageReferenceEndpoints.allImpl ++
        indexEndpoints.allImpl ++
        observationEndpoints.allImpl

    val docEndpoints: List[ServerEndpoint[Any, Future]] =
        SwaggerInterpreter().fromServerEndpoints(apiEndpoints, AppConfig.Name, AppConfig.Version)

    val prometheusMetrics: PrometheusMetrics[Future] = PrometheusMetrics.default[Future]()
    val metricsEndpoint: ServerEndpoint[Any, Future] = prometheusMetrics.metricsEndpoint

    val all: List[ServerEndpoint[Any, Future]] =
        apiEndpoints ++ docEndpoints ++ List(metricsEndpoint)

}
