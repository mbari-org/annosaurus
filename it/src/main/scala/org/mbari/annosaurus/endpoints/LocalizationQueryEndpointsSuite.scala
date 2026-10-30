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

package org.mbari.annosaurus.endpoints

import org.mbari.annosaurus.controllers.{LocalizationController, QueryController, TestUtils}
import org.mbari.annosaurus.domain.LocalizationSC
import org.mbari.annosaurus.etc.jwt.JwtService
import sttp.tapir.docs.openapi.OpenAPIDocsInterpreter
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.client3.testing.SttpBackendStub
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import scala.concurrent.Future
import org.mbari.annosaurus.domain.{ConstraintRequest, Count, QueryRequest}
import org.mbari.annosaurus.etc.circe.CirceCodecs.{*, given}
import org.mbari.annosaurus.repository.jpa.JPADAOFactory
import org.mbari.annosaurus.repository.query.JDBC
import sttp.client3.*
import sttp.model.StatusCode
import sttp.tapir.stringToPath
import org.mbari.annosaurus.etc.sdk.Futures.*

import java.nio.file.Files
import scala.jdk.CollectionConverters.*

/**
 * Tests for the localizations query routes, a second instance of [[QueryEndpoints]] over the localized_annotations view
 */
trait LocalizationQueryEndpointsSuite extends EndpointsSuite:

    given JPADAOFactory = daoFactory

    private val PartValue = "part-x-unique"

    // Creating the EntityManagerFactory runs the Flyway migrations that create the view
    private lazy val controller =
        daoFactory.entityManagerFactory
        new QueryController(daoFactory.databaseConfig, daoFactory.localizationView)
    private lazy val endpoints  =
        new QueryEndpoints(controller, "localizations" / "query", "localization", "Localization Query")

    /**
     * Two annotations (imaged moments), each with one observation holding a bounding_box, point, line and polygon.
     *   - im1 bbox: confidence 0.9, verifier alice-verifier, part PartValue
     *   - im2 bbox: confidence 0.2, verifier bob
     *   - everything else: confidence 0.5, verifier bob
     * @return
     *   the observation concepts, in seed order
     */
    private def seed(): Seq[String] =
        val xs = TestUtils.build(2, 1, 0, 0, false, 4)
        for (im, i) <- xs.zipWithIndex do
            for obs <- im.getObservations.asScala do
                for loc <- obs.getLocalizations.asScala do
                    loc.setVerifier("bob")
                    loc.setConfidence(0.5f)
                    if loc.getLocalizationType == "bounding_box" then
                        if i == 0 then
                            loc.setConfidence(0.9f)
                            loc.setVerifier("alice-verifier")
                            loc.setPart(PartValue)
                        else loc.setConfidence(0.2f)
        TestUtils.create(xs)
        xs.flatMap(_.getObservations.asScala.map(_.getConcept))

    private def post(
        impl: sttp.tapir.server.ServerEndpoint[Any, scala.concurrent.Future],
        path: String,
        request: QueryRequest
    ): String =
        var out = ""
        runPost(
            impl,
            s"http://test.com/v1/localizations/query/$path",
            request.stringify,
            response =>
                assertEquals(response.code, StatusCode.Ok)
                out = response.body.fold(e => fail(e), identity)
        )
        out

    /** @return (header, rows) */
    private def run(request: QueryRequest): (Seq[String], Seq[Seq[String]]) =
        val lines = post(endpoints.runQueryImpl, "run", request).split("\n").toSeq.filter(_.nonEmpty)
        (lines.headOption.map(_.split("\t", -1).toSeq).getOrElse(Nil), lines.drop(1).map(_.split("\t", -1).toSeq))

    private def count(request: QueryRequest): Long =
        var out = -1L
        runPost(
            endpoints.countImpl,
            "http://test.com/v1/localizations/query/count",
            request.stringify,
            response =>
                assertEquals(response.code, StatusCode.Ok, response.body.toString)
                out = checkResponse[Count](response.body).count
        )
        out

    private val select = Seq("localization_uuid", "localization_type", "part", "ancillary_x", "confidence", "verifier")

    private def where(c: ConstraintRequest) = QueryRequest(select = Some(select), where = Some(Seq(c)))

    test("columns lists localization columns and not association or image columns") {
        runGet(
            endpoints.listColumnsImpl,
            "http://test.com/v1/localizations/query/columns",
            response =>
                assertEquals(response.code, StatusCode.Ok, response.body.toString)
                val names = checkResponse[Seq[JDBC.Metadata]](response.body).map(_.columnName)
                assert(names.contains("localization_uuid"), names.toString)
                assert(!names.contains("link_name"), names.toString)
                assert(!names.contains("image_url"), names.toString)
        )
    }

    test("existing /v1/query/columns still lists link_name") {
        val annotationEndpoints = new QueryEndpoints(new QueryController(daoFactory.databaseConfig, daoFactory.annotationView))
        runGet(
            annotationEndpoints.listColumnsImpl,
            "http://test.com/v1/query/columns",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val names = checkResponse[Seq[JDBC.Metadata]](response.body).map(_.columnName)
                assert(names.contains("link_name"), names.toString)
        )
    }

    test("the two QueryEndpoints instances have distinct operation names") {
        val other = new QueryEndpoints(controller)
        val names = (endpoints.all ++ other.all).map(_.info.name.getOrElse(""))
        assertEquals(names.distinct.size, names.size)
        assertEquals(other.all.flatMap(_.info.name), Seq("listQueryColumns", "runQuery", "downloadTsv", "countQuery"))
    }

    test("run with localization_type = bounding_box returns exactly the bbox rows") {
        seed()
        val (header, rows) = run(where(ConstraintRequest("localization_type", equals = Some("bounding_box"))))
        assertEquals(header, select)
        assertEquals(rows.size, 2)
        assert(rows.forall(_(1) == "bounding_box"))
        assert(!header.contains("link_name"))
        assert(!header.contains("image_url"))
    }

    test("run with a confidence range filter") {
        seed()
        val (_, rows) = run(where(ConstraintRequest("confidence", minmax = Some(Seq(0.8, 1.0)))))
        assertEquals(rows.size, 1)
        assertEquals(rows.head(1), "bounding_box")
        val (_, rows2) = run(where(ConstraintRequest("confidence", min = Some(0.1), max = Some(0.3))))
        assertEquals(rows2.size, 1)
    }

    test("run with a verifier contains filter") {
        seed()
        val (_, rows) = run(where(ConstraintRequest("verifier", contains = Some("alice"))))
        assertEquals(rows.size, 1)
        val (_, rows2) = run(where(ConstraintRequest("verifier", like = Some("%bob"))))
        assertEquals(rows2.size, 7)
    }

    test("count equals the number of rows run returns") {
        seed()
        val request = where(ConstraintRequest("localization_type", equals = Some("bounding_box")))
        assertEquals(count(request), 2L)
        assertEquals(count(request), run(request)._2.size.toLong)
        // count, like the annotation query, requires a where clause
        val all     = where(ConstraintRequest("localization_uuid", isnull = Some(false)))
        assertEquals(count(all), 8L)
        assertEquals(run(all)._2.size, 8)
    }

    test("concept matches the observation concept only, not localization part") {
        val concepts = seed()
        val (_, byPart) = run(where(ConstraintRequest("concept", equals = Some(PartValue))))
        assertEquals(byPart.size, 0)
        val (_, byConcept) = run(where(ConstraintRequest("concept", equals = Some(concepts.head))))
        assertEquals(byConcept.size, 4)
        val (_, byPartColumn) = run(where(ConstraintRequest("part", equals = Some(PartValue))))
        assertEquals(byPartColumn.size, 1)
    }

    test("limit and offset paging") {
        seed()
        val base  = QueryRequest(select = Some(Seq("localization_uuid")), orderBy = Some(Seq("localization_uuid")))
        val all   = run(base)._2.map(_.head)
        assertEquals(all.size, 8)
        val page1 = run(base.copy(limit = Some(3), offset = Some(0)))._2.map(_.head)
        val page2 = run(base.copy(limit = Some(3), offset = Some(3)))._2.map(_.head)
        val page3 = run(base.copy(limit = Some(3), offset = Some(6)))._2.map(_.head)
        assertEquals(page1.size, 3)
        assertEquals(page2.size, 3)
        assertEquals(page3.size, 2)
        assertEquals((page1 ++ page2 ++ page3).distinct.size, 8)
        assertEquals(page1 ++ page2 ++ page3, all)
    }

    test("non-strict, concurrentObservations and relatedAssociations queries work on the view") {
        seed()
        val request = QueryRequest(
            select = Some(Seq("localization_uuid")),
            where = Some(Seq(ConstraintRequest("localization_type", equals = Some("point")))),
            strict = Some(false),
            concurrentObservations = Some(true),
            relatedAssociations = Some(true)
        )
        val (header, rows) = run(request)
        assert(header.contains("localization_uuid"))
        assertEquals(rows.size, 8)
    }

    test("download returns TSV with a header containing localization_uuid") {
        seed()
        val stub     = newBackendStub(endpoints.downloadTsvImpl)
        val request  = basicRequest
            .post(uri"http://test.com/v1/localizations/query/download")
            .body(QueryRequest(select = Some(select)).stringify)
        val response = request.send(stub).join
        assertEquals(response.code, StatusCode.Ok)
        val lines    = Files.readAllLines((response.body: Any).asInstanceOf[sttp.tapir.FileRange].file.toPath).asScala
        assert(lines.head.split("\t").contains("localization_uuid"), lines.head)
        assertEquals(lines.size, 9)
    }

    // ---- combined routes: localization routes + localization query routes in ONE interpreter ----

    private given jwtService: JwtService = new JwtService("mbari", "foo", "bar")
    private lazy val locEndpoints       = new LocalizationEndpoints(new LocalizationController(daoFactory))
    private lazy val defaultQueryEndpoints =
        new QueryEndpoints(new QueryController(daoFactory.databaseConfig, daoFactory.annotationView))

    private def combinedStub(es: List[ServerEndpoint[Any, Future]]) =
        TapirStubInterpreter(SttpBackendStub.asynchronousFuture).whenServerEndpointsRunLogic(es).backend()

    private def combinedChecks(label: String, es: List[ServerEndpoint[Any, Future]]): Unit =
        endpoints // force the EntityManagerFactory / migrations
        val stub    = combinedStub(es)
        val token   = jwtService.authorize("foo").getOrElse(fail("no jwt"))
        val obsUuid = TestUtils.create(1, 1).head.getObservations.iterator().next().getUuid
        def send(r: Request[Either[String, String], Any]) = r.send(stub).join

        val put = send(
            basicRequest
                .put(uri"http://test.com/v1/localizations/$obsUuid/bounding_box")
                .body("""{"localization_type":"bounding_box","x":0.1,"y":0.1,"width":0.2,"height":0.2}""")
                .auth
                .bearer(token)
        )
        assertEquals(put.code, StatusCode.Ok, s"$label: ${put.body}")
        val created = checkResponse[LocalizationSC](put.body)

        val get = send(basicRequest.get(uri"http://test.com/v1/localizations/${created.uuid.get}"))
        assertEquals(get.code, StatusCode.Ok, s"$label: ${get.body}")
        assertEquals(send(basicRequest.get(uri"http://test.com/v1/localizations/list/types")).code, StatusCode.Ok)
        assertEquals(
            send(basicRequest.get(uri"http://test.com/v1/localizations/query/columns")).code,
            StatusCode.Ok,
            label
        )

        val body = QueryRequest(
            select = Some(Seq("localization_uuid")),
            where = Some(Seq(ConstraintRequest("localization_type", equals = Some("bounding_box"))))
        ).stringify
        val cnt  = send(basicRequest.post(uri"http://test.com/v1/localizations/query/count").body(body))
        assertEquals(cnt.code, StatusCode.Ok, s"$label: ${cnt.body}")
        assertEquals(checkResponse[Count](cnt.body).count, 1L)
        val rn   = send(basicRequest.post(uri"http://test.com/v1/localizations/query/run").body(body))
        assertEquals(rn.code, StatusCode.Ok, s"$label: ${rn.body}")
        val lines = rn.body.fold(e => fail(e), identity).split("\n").toSeq
        // SQL Server renders uniqueidentifier in upper case
        assertEquals(lines.map(_.toLowerCase), Seq("localization_uuid", created.uuid.get.toString))
        val dl   = send(basicRequest.post(uri"http://test.com/v1/localizations/query/download").body(body))
        assertEquals(dl.code, StatusCode.Ok, label)
        // the default instance still answers at /v1/query when registered
        if es.contains(defaultQueryEndpoints.listColumnsImpl) then
            assertEquals(send(basicRequest.get(uri"http://test.com/v1/query/columns")).code, StatusCode.Ok)

    test("combined routes: query endpoints registered before localization endpoints") {
        combinedChecks(
            "query first",
            defaultQueryEndpoints.allImpl ++ endpoints.allImpl ++ locEndpoints.allImpl
        )
    }

    test("combined routes: localization endpoints registered before query endpoints") {
        combinedChecks(
            "localization first",
            locEndpoints.allImpl ++ endpoints.allImpl ++ defaultQueryEndpoints.allImpl
        )
    }

    test("OpenAPI docs build for the combined endpoint list and operationIds are unique") {
        // The real Endpoints object needs a production database config, so build from the same classes it wires up.
        val impls = defaultQueryEndpoints.allImpl ++ endpoints.allImpl ++ locEndpoints.allImpl
        SwaggerInterpreter().fromServerEndpoints[Future](impls, "test", "1.0")
        val openApi = OpenAPIDocsInterpreter().serverEndpointsToOpenAPI[Future](impls, "test", "1.0")
        val ids     = openApi.paths.pathItems.values.toSeq.flatMap(p =>
            Seq(p.get, p.put, p.post, p.delete, p.options, p.head, p.patch, p.trace).flatten.flatMap(_.operationId)
        )
        assert(ids.size >= 4 + 4 + 4, ids.toString)
        assertEquals(ids.distinct.size, ids.size, ids.toString)
        assert(ids.contains("localizationrunQuery") && ids.contains("runQuery"), ids.toString)
    }

    // ---- I5: boolean and uuid equality filters (spec 7.3) on both views ---------------------------------

    /**
     * Two imaged moments, one observation each, each with a bounding_box, point, line and polygon. Flags: im1 bbox
     * training, im1 point truncated, im2 line difficult; everything else false.
     * @return
     *   (observation uuids, uuid of the im1 bbox)
     */
    private def seedFlags(): (Seq[java.util.UUID], java.util.UUID) =
        val xs = TestUtils.build(2, 1, 0, 0, false, 4)
        for (im, i) <- xs.zipWithIndex; obs <- im.getObservations.asScala; loc <- obs.getLocalizations.asScala do
            // TestUtils randomizes the flags; clear them all, then set exactly the ones under test
            loc.setTraining(false)
            loc.setTruncated(false)
            loc.setDifficult(false)
            (i, loc.getLocalizationType) match
                case (0, "bounding_box") => loc.setTraining(true)
                case (0, "point")        => loc.setTruncated(true)
                case (1, "line")         => loc.setDifficult(true)
                case _                   => ()
        TestUtils.create(xs)
        val obs  = xs.flatMap(_.getObservations.asScala)
        val bbox = obs.head.getLocalizations.asScala.find(_.getLocalizationType == "bounding_box").get.getUuid
        (obs.map(_.getUuid), bbox)

    private def uuids(rows: Seq[Seq[String]]): Set[String] = rows.map(_.head.toLowerCase).toSet

    private def flagQuery(c: ConstraintRequest) =
        QueryRequest(select = Some(Seq("localization_uuid", "localization_type")), where = Some(Seq(c)))

    test("training equals true / false") {
        val (_, bbox) = seedFlags()
        val (_, t)    = run(flagQuery(ConstraintRequest("training", equals = Some("true"))))
        assertEquals(uuids(t), Set(bbox.toString))
        val (_, f)    = run(flagQuery(ConstraintRequest("training", equals = Some("false"))))
        assertEquals(f.size, 7)
        assertEquals(count(flagQuery(ConstraintRequest("training", equals = Some("true")))), 1L)
        assertEquals(count(flagQuery(ConstraintRequest("training", in = Some(Seq("true", "false"))))), 8L)
    }

    test("truncated and difficult equals true") {
        seedFlags()
        val (_, tr) = run(flagQuery(ConstraintRequest("truncated", equals = Some("true"))))
        assertEquals(tr.map(_(1)), Seq("point"))
        val (_, df) = run(flagQuery(ConstraintRequest("difficult", equals = Some("true"))))
        assertEquals(df.map(_(1)), Seq("line"))
        assertEquals(count(flagQuery(ConstraintRequest("difficult", equals = Some("false")))), 7L)
    }

    test("localization_uuid equals and observation_uuid equals / in on the localization view") {
        val (obs, bbox) = seedFlags()
        val (_, l)      = run(flagQuery(ConstraintRequest("localization_uuid", equals = Some(bbox.toString))))
        assertEquals(uuids(l), Set(bbox.toString))
        val (_, o1)     = run(flagQuery(ConstraintRequest("observation_uuid", equals = Some(obs.head.toString))))
        assertEquals(o1.size, 4)
        assert(uuids(o1).contains(bbox.toString))
        val (_, o2)     = run(flagQuery(ConstraintRequest("observation_uuid", in = Some(obs.map(_.toString)))))
        assertEquals(o2.size, 8)
        assertEquals(count(flagQuery(ConstraintRequest("observation_uuid", in = Some(obs.map(_.toString))))), 8L)
    }

    private lazy val annotationQueryEndpoints =
        new QueryEndpoints(new QueryController(daoFactory.databaseConfig, daoFactory.annotationView))

    private def runAnnotations(request: QueryRequest): Seq[Seq[String]] =
        var out = ""
        runPost(
            annotationQueryEndpoints.runQueryImpl,
            "http://test.com/v1/query/run",
            request.stringify,
            response =>
                assertEquals(response.code, StatusCode.Ok, response.body.toString)
                out = response.body.fold(e => fail(e), identity)
        )
        out.split("\n").toSeq.filter(_.nonEmpty).drop(1).map(_.split("\t", -1).toSeq)

    test("observation_uuid and imaged_moment_uuid equals / in on the annotations view") {
        endpoints // force migrations
        val xs   = TestUtils.create(2, 1, 1)
        val obs  = xs.flatMap(_.getObservations.asScala).map(_.getUuid)
        val sel  = Some(Seq("observation_uuid", "concept"))
        val one  = runAnnotations(
            QueryRequest(select = sel, where = Some(Seq(ConstraintRequest("observation_uuid", equals = Some(obs.head.toString)))))
        )
        assertEquals(uuids(one), Set(obs.head.toString))
        val both = runAnnotations(
            QueryRequest(select = sel, where = Some(Seq(ConstraintRequest("observation_uuid", in = Some(obs.map(_.toString))))))
        )
        assertEquals(uuids(both), obs.map(_.toString).toSet)
        val im   = runAnnotations(
            QueryRequest(
                select = Some(Seq("imaged_moment_uuid")),
                where = Some(Seq(ConstraintRequest("imaged_moment_uuid", equals = Some(xs.head.getUuid.toString))))
            )
        )
        assertEquals(uuids(im), Set(xs.head.getUuid.toString))
        // existing string, numeric and timestamp filters are unchanged on the same view
        val byConcept = runAnnotations(
            QueryRequest(select = sel, where = Some(Seq(ConstraintRequest("concept", equals = Some(xs.head.getObservations.asScala.head.getConcept)))))
        )
        assertEquals(byConcept.size, 1)
    }
