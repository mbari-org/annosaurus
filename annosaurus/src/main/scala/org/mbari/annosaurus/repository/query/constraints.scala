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
    constraints: Seq[Constraint],
    columns: Seq[String] = Seq.empty,
    limit: Option[Int] = None,
    offset: Option[Int] = None,
    concurrentObservations: Option[Boolean] = None,
    relatedAssociations: Option[Boolean] = None
)

object Query:
    def from(queryRequest: QueryRequest): Query =
        Query(
            constraints = queryRequest.where.map(Constraint.from),
            columns = queryRequest.select.getOrElse(Seq.empty),
            limit = queryRequest.limit,
            offset = queryRequest.offset,
            concurrentObservations = queryRequest.concurrentObservations,
            relatedAssociations = queryRequest.relatedAssociations
        )



sealed trait Constraint:
    def column: String
    def toPreparedStatementTemplate: String

    @throws[SQLException]
    def bind(statement: PreparedStatement, idx: Int): Int

object Constraint:

    def from (constraintRequest: ConstraintRequest): Constraint =
        constraintRequest match
            case ConstraintRequest(column, Some(in), _, _, _, _, _, _) => In(column, in)
            case ConstraintRequest(column, _, Some(like), _, _, _, _, _) => Like(column, like)
            case ConstraintRequest(column, _, _, Some(min), _, _, _, _) => Min(column, min)
            case ConstraintRequest(column, _, _, _, Some(max), _, _, _) => Max(column, max)
            case ConstraintRequest(column, _, _, _, _, Some(minmax), _, _) => MinMax(column, minmax.head, minmax(1))
            case ConstraintRequest(column, _, _, _, _, _, Some(between), _) => Date(column, between.head, between(1))
            case ConstraintRequest(column, _, _, _, _, _, _, Some(isnull)) => IsNull(column, isnull)
            case _ => Noop

    case object Noop extends Constraint:
        val column: String                                = ""
        def toPreparedStatementTemplate: String               = ""
        @throws[SQLException]
        def bind(statement: PreparedStatement, idx: Int): Int = idx

    case class Date(column: String, startTimestamp: Instant, endTimestamp: Instant)
        extends Constraint:
        @throws[SQLException]
        def bind(statement: PreparedStatement, idx: Int): Int =
            statement.setObject(idx, startTimestamp)
            statement.setObject(idx + 1, endTimestamp)
            idx + 2

        def toPreparedStatementTemplate: String = column + " BETWEEN ? AND ?"

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
            statement.setString(idx, s"%$like%")
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
//
//case class InConstraint[A](columnName: String, in: Seq[A]) extends Constraint {
//    require(columnName != null)
//    require(in != null && in.nonEmpty, "Check your value arg! null and empty values are not allowed")
//
//    @throws[SQLException]
//    def bind(statement: PreparedStatement, idx: Int): Int = {
//        var i = idx
//        for (v <- in) {
//            statement.setObject(idx, v)
//            i = i + 1
//        }
//        i
//    }
//
//    def toPreparedStatementTemplate: String = if (in.size eq 1) columnName + " = ?"
//    else columnName + " IN " + in.map(s => "?").mkString("(", ",", ")")
//}
//
//case class LikeConstraint(columnName: String, like: String) extends Constraint {
//    @throws[SQLException]
//    def bind(statement: PreparedStatement, idx: Int): Int = {
//        statement.setString(idx, s"%$like%")
//        idx + 1
//    }
//
//    def toPreparedStatementTemplate: String = s"$columnName LIKE ?"
//}
//
//case class MaxConstraint(columnName: String, max: Double) extends Constraint {
//    @throws[SQLException]
//    def bind(statement: PreparedStatement, idx: Int): Int = {
//        statement.setDouble(idx, max)
//        idx + 1
//    }
//
//    def toPreparedStatementTemplate: String = columnName + " <= ?"
//}
//
//case class MinConstraint(columnName: String, min: Double) extends Constraint {
//    @throws[SQLException]
//    def bind(statement: PreparedStatement, idx: Int): Int = {
//        statement.setDouble(idx, min)
//        idx + 1
//    }
//
//    def toPreparedStatementTemplate: String = columnName + " >= ?"
//}
//
//case class MinMaxConstraint(columnName: String, min: Double, max: Double) extends Constraint {
//    @throws[SQLException]
//    def bind(statement: PreparedStatement, idx: Int): Int = {
//        statement.setDouble(idx, min)
//        statement.setDouble(idx + 1, max)
//        idx + 2
//    }
//
//    def toPreparedStatementTemplate: String = columnName + " BETWEEN ? AND ?"
//}
//
//case class DateConstraint(columnName: String, startTimestamp: Instant, endTimestamp: Instant) extends Constraint:
//    @throws[SQLException]
//    def bind(statement: PreparedStatement, idx: Int): Int = {
//        statement.setObject(idx, min)
//        statement.setObject(idx + 1, max)
//        idx + 2
//    }
//
//    def toPreparedStatementTemplate: String = columnName + " BETWEEN ? AND ?"
//
//
