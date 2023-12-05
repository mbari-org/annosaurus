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

package org.mbari.vars.annotation.auth

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.DispatcherType
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.Part
import java.{util => ju}
import java.util.Locale
import java.security.Principal
import java.util.Locale
import jakarta.servlet.http.Part
import java.util.Collection
import jakarta.servlet.http.HttpUpgradeHandler
import jakarta.servlet.RequestDispatcher
import java.io.BufferedReader
import jakarta.servlet.http.HttpSession
import jakarta.servlet.ServletInputStream
import java.{util => ju}
import jakarta.servlet.{AsyncContext, ServletRequest, ServletResponse}
import jakarta.servlet.AsyncContext
import jakarta.servlet.ServletContext
import com.typesafe.config.ConfigFactory

class MockJwtHttpServletRequest(tokenType: String, token: String) extends HttpServletRequest {

  val config = ConfigFactory.load()
  val headers = Map("Authorization" -> s"$tokenType $token")

  override def getAttribute(name: String): Object = ???

  override def getAttributeNames(): ju.Enumeration[String] = ???

  override def getCharacterEncoding(): String = ???

  override def setCharacterEncoding(env: String): Unit = ???

  override def getContentLength(): Int = ???

  override def getContentLengthLong(): Long = ???

  override def getContentType(): String = ???

  override def getInputStream(): ServletInputStream = ???

  override def getParameter(name: String): String = ???

  override def getParameterNames(): ju.Enumeration[String] = ???

  override def getParameterValues(name: String): Array[String] = ???

  override def getParameterMap(): ju.Map[String,Array[String]] = ???

  override def getProtocol(): String = ???

  override def getScheme(): String = ???

  override def getServerName(): String = ???

  override def getServerPort(): Int = ???

  override def getReader(): BufferedReader = ???

  override def getRemoteAddr(): String = ???

  override def getRemoteHost(): String = ???

  override def setAttribute(name: String, o: Object): Unit = ???

  override def removeAttribute(name: String): Unit = ???

  override def getLocale(): Locale = ???

  override def getLocales(): ju.Enumeration[Locale] = ???

  override def isSecure(): Boolean = ???

  override def getRequestDispatcher(path: String): RequestDispatcher = ???

  override def getRealPath(path: String): String = ???

  override def getRemotePort(): Int = ???

  override def getLocalName(): String = ???

  override def getLocalAddr(): String = ???

  override def getLocalPort(): Int = ???

  override def getServletContext(): ServletContext = ???

  override def startAsync(): AsyncContext = ???

  override def startAsync(servletRequest: ServletRequest, servletResponse: ServletResponse): AsyncContext = ???

  override def isAsyncStarted(): Boolean = ???

  override def isAsyncSupported(): Boolean = ???

  override def getAsyncContext(): AsyncContext = ???

  override def getDispatcherType(): DispatcherType = ???

  override def getAuthType(): String = ???

  override def getCookies(): Array[Cookie] = ???

  override def getDateHeader(name: String): Long = ???

  override def getHeader(name: String): String = headers(name)

  override def getHeaders(name: String): ju.Enumeration[String] = ???

  override def getHeaderNames(): ju.Enumeration[String] = ???

  override def getIntHeader(name: String): Int = ???

  override def getMethod(): String = ???

  override def getPathInfo(): String = ???

  override def getPathTranslated(): String = ???

  override def getContextPath(): String = ???

  override def getQueryString(): String = ???

  override def getRemoteUser(): String = ???

  override def isUserInRole(role: String): Boolean = ???

  override def getUserPrincipal(): Principal = ???

  override def getRequestedSessionId(): String = ???

  override def getRequestURI(): String = ???

  override def getRequestURL(): StringBuffer = ???

  override def getServletPath(): String = ???

  override def getSession(create: Boolean): HttpSession = ???

  override def getSession(): HttpSession = ???

  override def changeSessionId(): String = ???

  override def isRequestedSessionIdValid(): Boolean = ???

  override def isRequestedSessionIdFromCookie(): Boolean = ???

  override def isRequestedSessionIdFromURL(): Boolean = ???

  override def isRequestedSessionIdFromUrl(): Boolean = ???

  override def authenticate(response: HttpServletResponse): Boolean = ???

  override def login(username: String, password: String): Unit = ???

  override def logout(): Unit = ???

  override def getParts(): Collection[Part] = ???

  override def getPart(name: String): Part = ???

  override def upgrade[T <: HttpUpgradeHandler](handlerClass: Class[T]): T = ???

  
}
