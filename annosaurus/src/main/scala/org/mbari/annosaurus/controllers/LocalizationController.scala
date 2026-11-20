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

package org.mbari.annosaurus.controllers

import io.reactivex.rxjava3.subjects.Subject
import jakarta.persistence.OptimisticLockException
import org.hibernate.StaleObjectStateException
import org.mbari.annosaurus.domain.{Localization, LocalizationRules}
import org.mbari.annosaurus.etc.jdk.Loggers.given
import org.mbari.annosaurus.etc.rxjava.EventBus
import org.mbari.annosaurus.messaging.Publisher
import org.mbari.annosaurus.repository.jpa.JPADAOFactory
import org.mbari.annosaurus.repository.jpa.entity.{LocalizationEntity, ObservationEntity}
import org.mbari.annosaurus.repository.{LocalizationDAO, NotFoundInDatastoreException}

import java.time.{Duration, Instant}
import java.util.UUID
import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future}

/** Raised when the caller's lastUpdated does not match the stored row. Carries the current row. */
final class StaleLocalizationException(val current: Localization)
    extends RuntimeException("Localization was modified since lastUpdated; fetch the current version and retry")

/** Raised when a single localization fails validation. */
final class LocalizationValidationException(val errors: Seq[String])
    extends IllegalArgumentException(errors.mkString("; "))

/** Raised when any item of a bulk upsert is invalid or stale. Nothing is written. */
final class BulkLocalizationException(val failures: Seq[(Int, Seq[String])])
    extends RuntimeException(
        failures.map((i, es) => s"item $i: ${es.mkString(", ")}").mkString("; ")
    )

class LocalizationController(
    val daoFactory: JPADAOFactory,
    bus: Subject[Any] = EventBus.RxSubject
) extends BaseController[LocalizationEntity, LocalizationDAO[LocalizationEntity], Localization]:

    type LDAO = LocalizationDAO[LocalizationEntity]

    /** Why a single upsert could not be applied. Nothing was written for it. */
    private enum Failure:
        case ObservationNotFound(uuid: UUID)
        case Stale(current: Localization)

    private val log       = System.getLogger(getClass.getName)
    private val publisher = Publisher(bus)

    override def newDAO(): LDAO = daoFactory.newLocalizationDAO()

    override def transform(a: LocalizationEntity): Localization = Localization.from(a)

    /** Copies every client-settable field from a transient entity onto a managed one. The uuid is kept. */
    private def copyFields(from: LocalizationEntity, to: LocalizationEntity): Unit =
        to.setX(from.getX)
        to.setY(from.getY)
        to.setWidth(from.getWidth)
        to.setHeight(from.getHeight)
        to.setVertices(from.getVertices)
        to.setMaskEncoding(from.getMaskEncoding)
        to.setMaskData(from.getMaskData)
        to.setMaskWidthPixels(from.getMaskWidthPixels)
        to.setMaskHeightPixels(from.getMaskHeightPixels)
        to.setGenerator(from.getGenerator)
        to.setConfidence(from.getConfidence)
        to.setObserver(from.getObserver)
        to.setObservationTimestamp(from.getObservationTimestamp)
        to.setVerifier(from.getVerifier)
        to.setVerificationTimestamp(from.getVerificationTimestamp)
        to.setComment(from.getComment)
        to.setPart(from.getPart)
        to.setTraining(from.isTraining)
        to.setTruncated(from.isTruncated)
        to.setDifficult(from.isDifficult)

    /**
     * Insert or update on the natural key (observation, type). Must run inside a transaction. Left explains why nothing was
     * written (unknown observation, or stale lastUpdated with the current row).
     */
    private def upsertEntity(
        dao: LDAO,
        observationUuid: UUID,
        l: Localization
    ): Either[Failure, LocalizationEntity] =
        val obsDao = daoFactory.newObservationDAO(dao)
        obsDao.findByUUID(observationUuid) match
            case None      => Left(Failure.ObservationNotFound(observationUuid))
            case Some(obs) => upsertEntity(dao, obs, observationUuid, l)

    private def upsertEntity(
        dao: LDAO,
        obs: ObservationEntity,
        observationUuid: UUID,
        l: Localization
    ): Either[Failure, LocalizationEntity] =
        val n = l.toEntity
        dao.findByObservationUuidAndType(observationUuid, l.localizationType) match
            case None           =>
                // The uuid is always server generated; the natural key is the identity
                n.setUuid(null)
                obs.addLocalization(n)
                dao.create(n)
                Right(n)
            case Some(existing) =>
                val stale = for
                    expected <- l.lastUpdated
                    actual   <- Option(existing.getLastUpdatedTime).map(_.toInstant)
                    if !LocalizationController.sameInstant(expected, actual)
                yield expected
                if stale.isDefined then Left(Failure.Stale(transform(existing)))
                else
                    if existing.getGenerator != n.getGenerator || existing.getVerifier != n.getVerifier then
                        log.atWarn.log(
                            s"Localization ${existing.getUuid} (${l.localizationType}) on observation $observationUuid " +
                                s"was replaced; generator ${existing.getGenerator} -> ${n.getGenerator}, " +
                                s"verifier ${existing.getVerifier} -> ${n.getVerifier}"
                        )
                    copyFields(n, existing)
                    Right(existing)

    private def validate(l: Localization): Either[Seq[String], Localization] =
        LocalizationRules.normalize(l)

    def upsert(observationUuid: UUID, localizationType: String, localization: Localization)(implicit
        ec: ExecutionContext
    ): Future[Localization] =
        validate(localization.copy(localizationType = localizationType)) match
            case Left(errors) => Future.failed(new LocalizationValidationException(errors))
            case Right(l)     =>
                def fn(dao: LDAO): Localization =
                    upsertEntity(dao, observationUuid, l) match
                        case Left(Failure.ObservationNotFound(u)) =>
                            throw new NotFoundInDatastoreException(s"Observation with UUID of $u not found")
                        case Left(Failure.Stale(current))         => throw new StaleLocalizationException(current)
                        case Right(entity)                        =>
                            // flush so the uuid and version are assigned (and constraint violations surface here)
                            dao.flush()
                            transform(entity)
                exec(fn)
                    .recoverWith {
                        case e if LocalizationController.isConcurrentWrite(e) =>
                            // Lost a race with another writer: report the winner's row as a conflict
                            execReadOnly(dao => dao.findByObservationUuidAndType(observationUuid, localizationType))
                                .flatMap {
                                    case Some(current) =>
                                        Future.failed(new StaleLocalizationException(transform(current)))
                                    case None          => Future.failed(e)
                                }
                    }
                    .map(a =>
                        publisher.created(a)
                        a
                    )

    def delete(observationUuid: UUID, localizationType: String)(implicit
        ec: ExecutionContext
    ): Future[Boolean] =
        def fn(dao: LDAO): Boolean =
            dao.findByObservationUuidAndType(observationUuid, localizationType) match
                case Some(entity) =>
                    Option(entity.getObservation).foreach(_.removeLocalization(entity))
                    dao.delete(entity)
                    true
                case None         => false
        exec(fn)

    def findByObservationUuid(uuid: UUID)(implicit ec: ExecutionContext): Future[Seq[Localization]] =
        execReadOnly(dao => dao.findByObservationUuid(uuid).map(transform))

    def findByVideoReferenceUuid(uuid: UUID, limit: Option[Int], offset: Option[Int])(implicit
        ec: ExecutionContext
    ): Future[Seq[Localization]] =
        val l = LocalizationController.pageLimit(limit)
        execReadOnly(dao => dao.findByVideoReferenceUuid(uuid, l, offset).map(transform))

    def bulkUpsert(items: Seq[Localization])(implicit ec: ExecutionContext): Future[Seq[Localization]] =
        val validated = items.map(l =>
            l.observationUuid match
                case None    => Left(Seq("observationUuid is required"))
                case Some(_) => validate(l)
        )
        // Failures known before touching the database, by item index
        val seen      = scala.collection.mutable.Set[(UUID, String)]()
        val upFront   = items.zip(validated).zipWithIndex.flatMap { case ((raw, v), i) =>
            val dup = raw.observationUuid.exists(o => !seen.add((o, raw.localizationType)))
            val msgs = v.left.getOrElse(Nil) ++ (if dup then Seq("duplicate key in batch") else Nil)
            if msgs.isEmpty then None else Some((i, msgs))
        }
        val upFrontIndexes = upFront.map(_._1).toSet

        def fn(dao: LDAO): Seq[Localization] =
            val outcomes = validated.zipWithIndex.map { case (v, i) =>
                if upFrontIndexes.contains(i) then None
                else
                    val l = v.toOption.get
                    Some(i -> upsertEntity(dao, l.observationUuid.get, l))
            }
            val dbFailures = outcomes.flatten.collect {
                case (i, Left(Failure.ObservationNotFound(u))) => (i, Seq(s"observation $u not found"))
                case (i, Left(Failure.Stale(_)))               =>
                    (i, Seq("stale: lastUpdated does not match the stored localization"))
            }
            val failures   = (upFront ++ dbFailures).sortBy(_._1)
            // Throwing rolls back the whole transaction
            if failures.nonEmpty then throw new BulkLocalizationException(failures)
            dao.flush()
            outcomes.flatten.map((_, r) => transform(r.toOption.get))

        exec(fn)
            .recoverWith {
                case e if LocalizationController.isConcurrentWrite(e) =>
                    Future.failed(
                        new BulkLocalizationException(
                            items.indices.map(i => (i, Seq("concurrent modification; retry")))
                        )
                    )
            }
            .map(xs =>
                xs.foreach(publisher.created)
                xs
            )

object LocalizationController:

    /** Page size of GET /v1/localizations/videoreference/{uuid} when no limit is given. */
    val DefaultPageLimit: Int = 1000

    /** Largest page of GET /v1/localizations/videoreference/{uuid}; larger limits are clamped, not rejected. */
    val MaxPageLimit: Int = 10000

    /** An absent (or negative) limit becomes the default; a limit above the maximum is clamped to it. */
    def pageLimit(limit: Option[Int]): Option[Int] =
        Some(limit.filter(_ >= 0).fold(DefaultPageLimit)(l => math.min(l, MaxPageLimit)))

    private val SameInstantTolerance = Duration.ofMillis(1)

    /**
     * True when two versions are "the same" stale-write token: less than 1 ms apart.
     *
     * The in-memory @Version can carry more precision than the column: on a JVM with a nanosecond clock (Linux),
     * Hibernate's SQL Server timestamp precision is 7 (100 ns) while the column is datetime2(6), and SQL Server ROUNDS
     * (e.g. .3459996 is stored as .346000). Truncating both sides to milliseconds then compares .345 with .346 and
     * reports a spurious conflict whenever the rounding crosses a millisecond boundary. A tolerance comparison is immune
     * to both truncation (PostgreSQL) and rounding (SQL Server). As with the earlier millisecond truncation, two writes
     * of the same row less than 1 ms apart cannot be told apart.
     */
    private[controllers] def sameInstant(a: Instant, b: Instant): Boolean =
        Duration.between(a, b).abs.compareTo(SameInstantTolerance) < 0

    private val PostgresUniqueViolation = "23505"
    private val SqlServerUniqueErrors   = Set(2627, 2601) // unique constraint / unique index

    /**
     * True when the failure is an optimistic lock failure (concurrent update, surfaced by Hibernate's @Version) or a
     * UNIQUE violation (concurrent first insert for the same observation and type). Foreign key and check violations
     * are not races and are deliberately not matched.
     */
    private[controllers] def isConcurrentWrite(e: Throwable): Boolean =
        @tailrec
        def loop(t: Throwable, depth: Int): Boolean =
            if t == null || depth > 16 then false
            else
                t match
                    case _: OptimisticLockException | _: StaleObjectStateException => true
                    case sql: java.sql.SQLException
                        if sql.getSQLState == PostgresUniqueViolation || SqlServerUniqueErrors.contains(
                            sql.getErrorCode
                        ) =>
                        true
                    case _                                                         => loop(t.getCause, depth + 1)
        loop(e, 0)
