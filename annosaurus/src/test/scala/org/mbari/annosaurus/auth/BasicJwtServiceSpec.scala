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

package org.mbari.annosaurus.auth

import com.typesafe.config.ConfigFactory
import io.circe.*
import io.circe.parser.*
import io.circe.generic.semiauto.*
import org.mbari.annosaurus.Constants
import org.mbari.annosaurus.etc.circe.CirceCodecs.{given, *}
import org.mbari.annosaurus.domain.{Authorization, AuthorizationSC}
import org.mbari.vars.annotation.auth.BasicJwtService
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import java.time.Duration

class BasicJwtServiceSpec extends AnyFunSpec with Matchers {


  val config = ConfigFactory.load()
  val apikey = config.getString("basicjwt.client.secret")
  val gson = Constants.GSON


  describe("BasicJwtService") {
    it("should authorize") {
      val service = new BasicJwtService()
      val opt = service.requestAuthorization(new MockJwtHttpServletRequest("APIKEY", apikey))
      opt.isDefined shouldBe true
      opt.get.reify[Authorization] match {
          case Left(_) => fail("Failed to decode authorization: " + opt.get)
          case Right(auth) =>
              service.isValid(Some(auth)) shouldBe true
      }
      decode[Authorization](opt.get) match {
        case Left(_) => fail("Failed to decode authorization: " + opt.get)
        case Right(auth) => 
          service.isValid(Some(auth)) shouldBe true
      }
    }

    it ("should fail on expired tokens") {
      val service = new BasicJwtService(Duration.ofSeconds(1))
      val opt = service.requestAuthorization(new MockJwtHttpServletRequest("APIKEY", apikey))
      opt.isDefined shouldBe true
      decode[Authorization](opt.get) match {
        case Left(_) => fail("Failed to decode authorization: " + opt.get)
        case Right(auth) =>
          val newRequest = new MockJwtHttpServletRequest("Bearer", auth.accessToken)
          service.validateAuthorization(newRequest) shouldBe true
          Thread.sleep(1000)
          service.validateAuthorization(newRequest) shouldBe false
      }
      
    }
  }


  
}
