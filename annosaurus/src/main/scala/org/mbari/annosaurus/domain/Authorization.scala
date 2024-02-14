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

package org.mbari.annosaurus.domain

final case class Authorization(tokenType: String, accessToken: String)
    extends ToSnakeCase[AuthorizationSC] {
    override def toSnakeCase: AuthorizationSC = AuthorizationSC(tokenType, accessToken)
}

final case class AuthorizationSC(token_type: String, access_token: String)
    extends ToCamelCase[Authorization] {
    override def toCamelCase: Authorization = Authorization(token_type, access_token)
}

object Authorization {
    val TokenTypeBearer: String = "Bearer"
    val TokenTypeApiKey: String = "APIKey"

    def bearer(accessToken: String): Authorization = Authorization(TokenTypeBearer, accessToken)
}
