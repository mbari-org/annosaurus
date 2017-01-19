package org.mbari.vars.annotation.auth

import java.lang.{Long => JLong}
import java.time.Instant
import javax.servlet.http.HttpServletRequest

import com.auth0.jwt.{JWTSigner, JWTVerifier}
import com.typesafe.config.ConfigFactory

/**
  * To use this authentication. The client and server should both have a shared
  * secret (aka client secret). The client sends this to the server in a
  * authorization header. If the secret is correct, the server will send back
  * a JWT token that can be used to validate subsequent requests.
  *
  * {{{
  *   Client                                                                Server
  *     |-------> POST /authorize: Authorization: APIKEY <client_secret> ----->|
  *     |                                                                      |
  *     |<------- {'access_token': <token>, 'token_type': 'Bearer'}     <------|
  *     |                                                                      |
  *     |                                                                      |
  *     |-------> POST /somemethod: Authorization: Bearer <token>       ------>|
  *     |                                                                      |
  *     |<------- 200                                                   <------|
  * }}}
  * @author Brian Schlining
  * @since 2017-01-18T16:42:00
  */
class BasicJwtService extends  AuthorizationService {

  private[this] val config = ConfigFactory.load()
  private[this] val issuer = config.getString("basicjwt.issuer")
  private[this] val apiKey = config.getString("basicjwt.client.secret")
  private[this] val signingSecret = config.getString("basicjwt.signing.secret")
  private[this] val verifier = new JWTVerifier(signingSecret)

  private def authorize(request: HttpServletRequest): Option[Authorization] = {
    Option(request.getHeader("Authorization"))
      .map(parseAuthHeader)
  }

  private def isValid(auth: Option[Authorization]): Boolean = auth match {
    case None => false
    case Some(a) =>
      if (a.tokenType.equalsIgnoreCase("BEARER")) {
        val claims = verifier.verify(a.accessToken)
        val exp = claims.getOrDefault("exp", 0L).asInstanceOf[Long]
        val expiration = Instant.ofEpochSecond(exp)
        val now = Instant.now()
        expiration.isAfter(now)
      }
      else false
  }

  private def parseAuthHeader(header: String): Authorization = {
    val parts = header.split("\\s")
    val tokenType = if (parts.length == 1) "undefined" else parts(0)
    val accessToken = if (parts.length == 1) parts(0) else parts(1)
    Authorization(tokenType, accessToken)
  }

  override def validateAuthorization(request: HttpServletRequest): Boolean = isValid(authorize(request))

  override def requestAuthorization(request: HttpServletRequest): Option[String] = {
    Option(request.getHeader("Authorization"))
        .map(parseAuthHeader)
        .filter(_.tokenType.equalsIgnoreCase("APIKEY"))
        .filter(_.accessToken == apiKey)
        .map(a => {
          val iat: JLong = System.currentTimeMillis() / 1000L // issued at claim
          val exp: JLong = iat + 86400L // expires claim. In this case the token expires in 24 hours

          val signer = new JWTSigner(signingSecret)
          val claims = new java.util.HashMap[String, Object]()
          claims.put("iss", issuer)
          claims.put("exp", exp)
          claims.put("iat", iat)

          signer.sign(claims)
        })
  }
}
