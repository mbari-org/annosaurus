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

import jakarta.persistence.{OptimisticLockException, PersistenceException, RollbackException}

import java.sql.SQLException

class LocalizationConcurrencySuite extends munit.FunSuite:

    private def check(e: Throwable): Boolean = LocalizationController.isConcurrentWrite(e)

    test("unique violations are concurrent writes") {
        assert(check(new SQLException("dup", "23505")))
        assert(check(new SQLException("dup", "23000", 2627)))
        assert(check(new SQLException("dup", "23000", 2601)))
    }

    test("foreign key and check violations are not concurrent writes") {
        assert(!check(new SQLException("fk", "23503")))
        assert(!check(new SQLException("ck", "23514")))
        assert(!check(new SQLException("fk or ck", "23000", 547)))
    }

    test("optimistic lock failures are concurrent writes") {
        assert(check(new OptimisticLockException("stale")))
    }

    test("wrapped causes are unwrapped") {
        val wrapped = new RollbackException("commit", new PersistenceException("flush", new SQLException("dup", "23505")))
        assert(check(wrapped))
        val fk      = new RollbackException("commit", new PersistenceException("flush", new SQLException("fk", "23503")))
        assert(!check(fk))
    }

    test("unrelated exceptions are not concurrent writes") {
        assert(!check(new RuntimeException("boom")))
    }

    // ---- I3: stale-token comparison tolerates rounding across a millisecond boundary ----------------

    private def same(a: String, b: String): Boolean =
        LocalizationController.sameInstant(java.time.Instant.parse(a), java.time.Instant.parse(b))

    test("an in-memory 100ns version and its rounded datetime2(6) value are the same instant") {
        // SQL Server rounds .3459996 to .346000 when the column is datetime2(6)
        assert(same("2026-01-01T00:00:00.3459996Z", "2026-01-01T00:00:00.346000Z"))
        assert(same("2026-01-01T00:00:00.346000Z", "2026-01-01T00:00:00.3459996Z"))
    }

    test("identical instants are the same") {
        assert(same("2026-01-01T00:00:00.123456Z", "2026-01-01T00:00:00.123456Z"))
    }

    test("instants 2 ms apart are not the same") {
        assert(!same("2026-01-01T00:00:00.123Z", "2026-01-01T00:00:00.125Z"))
        assert(!same("2026-01-01T00:00:00.125Z", "2026-01-01T00:00:00.123Z"))
    }
