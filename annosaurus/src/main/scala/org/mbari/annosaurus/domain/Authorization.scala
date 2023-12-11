package org.mbari.annosaurus.domain

final case class Authorization(token_type: String, access_token: String) {

    def tokenType: String   = token_type
    def accessToken: String = access_token
}

object Authorization {
    val TokenTypeBearer: String = "Bearer"
    val TokenTypeApiKey: String = "APIKey"

    def bearer(accessToken: String): Authorization = Authorization(TokenTypeBearer, accessToken)
}
