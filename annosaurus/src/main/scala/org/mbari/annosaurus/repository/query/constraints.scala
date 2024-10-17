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

package org.mbari.annosaurus.repository.query

import org.mbari.annosaurus.domain.{ConstraintRequest, QueryRequest}

import java.sql.{PreparedStatement, SQLException}
import java.time.Instant
import org.mbari.annosaurus.etc.circe.CirceCodecs.{*, given}

// Define the Root case class
case class Query(
    where: Seq[Constraint],
    select: Seq[String] = Seq.empty,
    limit: Option[Int] = None,
    offset: Option[Int] = None,
    concurrentObservations: Boolean = false,
    relatedAssociations: Boolean = false,
    distinct: Boolean = true,
    strict: Boolean = false,
    orderby: Option[Seq[String]] = None
)

object Query:

    def validate(queryRequest: QueryRequest, checkWhere: Boolean = true): Either[Throwable, Query] =
        val query = from(queryRequest)
        if (checkWhere && query.where.isEmpty) then Left(new IllegalArgumentException("where clause is required"))
        else if (query.select.isEmpty) then Left(new IllegalArgumentException("select clause is required"))
        else if (query.strict && (query.concurrentObservations || query.relatedAssociations)) then
            Left(new IllegalArgumentException("strict mode cannot be used with concurrentObservations or relatedAssociations"))
        else
            Right(from(queryRequest))

    def from(queryRequest: QueryRequest): Query =
        Query(
            where = queryRequest.where.map(Constraint.from),
            select = queryRequest.select.getOrElse(Seq.empty),
            limit = queryRequest.limit,
            offset = queryRequest.offset,
            concurrentObservations = queryRequest.concurrentObservations.getOrElse(false),
            relatedAssociations = queryRequest.relatedAssociations.getOrElse(false),
            distinct = queryRequest.distinct.getOrElse(true),
            strict = queryRequest.strict.getOrElse(false),
            orderby = queryRequest.orderby
        )



sealed trait Constraint:
    def column: String
    def toPreparedStatementTemplate: String

    @throws[SQLException]
    def bind(statement: PreparedStatement, idx: Int): Int

object Constraint:

    def from (constraintRequest: ConstraintRequest): Constraint =
        constraintRequest match
            case ConstraintRequest(column, Some(between: Seq[Instant]), _, _, _, _, _, _, _, _) => Date(column, between.head, between(1))
            case ConstraintRequest(column, _, Some(contains), _, _, _, _, _, _, _) => Contains(column, contains)
            case ConstraintRequest(column, _, _, Some(equals), _, _, _, _, _, _) => Equals(column, equals)
            case ConstraintRequest(column, _, _, _,Some(in), _, _, _, _, _) => In(column, in)
            case ConstraintRequest(column, _, _, _, _, Some(isnull), _, _, _, _) => IsNull(column, isnull)
            case ConstraintRequest(column, _, _, _, _, _, Some(like), _, _, _) => Like(column, like)
            case ConstraintRequest(column, _, _, _, _, _, _, Some(max), _, _) => Max(column, max)
            case ConstraintRequest(column, _, _, _, _, _, _, _, Some(min), _) => Min(column, min)
            case ConstraintRequest(column, _, _, _, _, _, _, _, _, Some(minmax)) => MinMax(column, minmax.head, minmax(1))
            case _ => Noop

    case object Noop extends Constraint:
        val column: String                                = ""
        def toPreparedStatementTemplate: String               = ""
        @throws[SQLException]
        def bind(statement: PreparedStatement, idx: Int): Int = idx

    case class Contains(column: String, contains: String) extends Constraint:
        @throws[SQLException]
        def bind(statement: PreparedStatement, idx: Int): Int =
            statement.setString(idx, s"%$contains%")
            idx + 1

        def toPreparedStatementTemplate: String = s"$column LIKE ?"

    case class Date(column: String, startTimestamp: Instant, endTimestamp: Instant)
        extends Constraint:
        @throws[SQLException]
        def bind(statement: PreparedStatement, idx: Int): Int =
            statement.setObject(idx, startTimestamp)
            statement.setObject(idx + 1, endTimestamp)
            idx + 2

        def toPreparedStatementTemplate: String = column + " BETWEEN ? AND ?"

    case class Equals[A](column: String, equals: A) extends Constraint:
        require(column != null)

        @throws[SQLException]
        def bind(statement: PreparedStatement, idx: Int): Int =
            statement.setObject(idx, equals)
            idx + 1

        def toPreparedStatementTemplate: String = column + " = ?"

    case class In[A](column: String, in: Seq[A]) extends Constraint:
        require(column != null)
        require(
            in != null && in.nonEmpty,
            "Check your value arg! null and empty values are not allowed"
        )

        @throws[SQLException]
        def bind(statement: PreparedStatement, idx: Int): Int =
            var i = idx
            for v <- in do
                statement.setObject(i, v)
                i = i + 1
            i

        def toPreparedStatementTemplate: String =
            if in.size eq 1 then column + " = ?"
            else column + " IN " + in.map(s => "?").mkString("(", ",", ")")

    case class Like(column: String, like: String) extends Constraint:
        @throws[SQLException]
        def bind(statement: PreparedStatement, idx: Int): Int =
            statement.setString(idx, like)
            idx + 1

        def toPreparedStatementTemplate: String = s"$column LIKE ?"

    case class Max(column: String, max: Double) extends Constraint:
        @throws[SQLException]
        def bind(statement: PreparedStatement, idx: Int): Int =
            statement.setDouble(idx, max)
            idx + 1

        def toPreparedStatementTemplate: String = column + " <= ?"

    case class Min(column: String, min: Double) extends Constraint:
        @throws[SQLException]
        def bind(statement: PreparedStatement, idx: Int): Int =
            statement.setDouble(idx, min)
            idx + 1

        def toPreparedStatementTemplate: String = column + " >= ?"

    case class MinMax(column: String, min: Double, max: Double) extends Constraint:
        @throws[SQLException]
        def bind(statement: PreparedStatement, idx: Int): Int =
            statement.setDouble(idx, min)
            statement.setDouble(idx + 1, max)
            idx + 2

        def toPreparedStatementTemplate: String = column + " BETWEEN ? AND ?"

    case class IsNull(column: String, isNull: Boolean) extends Constraint:
        @throws[SQLException]
        def bind(statement: PreparedStatement, idx: Int): Int = idx

        def toPreparedStatementTemplate: String = if isNull then column + " IS NULL"
        else column + " IS NOT NULL"
