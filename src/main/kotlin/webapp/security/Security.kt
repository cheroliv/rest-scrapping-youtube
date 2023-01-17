package webapp.security

import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.jackson.io.JacksonSerializer
import io.jsonwebtoken.lang.Strings
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.InitializingBean
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.stereotype.Component
import webapp.Constants
import webapp.Logging
import webapp.ApplicationProperties
import java.security.Key
import java.util.*

@Component
class Security(
    private val properties: ApplicationProperties,
    private var key: Key? = null,
    private var tokenValidityInMilliseconds: Long = 0,
    private var tokenValidityInMillisecondsForRememberMe: Long = 0
) : InitializingBean {


    @Throws(Exception::class)
    override fun afterPropertiesSet() {
        properties
            .security
            .authentication
            .jwt
            .secret
            .run {
                key = Keys.hmacShaKeyFor(
                    when {
                        !Strings.hasLength(this) -> Logging.w(
                            "Warning: the Jwt key used is not Base64-encoded. " +
                                    "We recommend using the `webapp.security.authentication.jwt.base64-secret`" +
                                    " key for optimum security."
                        ).run { toByteArray(Charsets.UTF_8) }

                        else -> Logging.d("Using a Base64-encoded Jwt secret key").run {
                            Decoders.BASE64.decode(
                                properties
                                    .security
                                    .authentication
                                    .jwt
                                    .base64Secret
                            )
                        }
                    }
                )
            }
        tokenValidityInMilliseconds = properties
            .security
            .authentication
            .jwt
            .tokenValidityInSeconds * 1000
        tokenValidityInMillisecondsForRememberMe = properties
            .security
            .authentication
            .jwt
            .tokenValidityInSecondsForRememberMe * 1000
    }

    suspend fun createToken(
        authentication: Authentication,
        rememberMe: Boolean
    ): String {
        Date().time.apply {
            return@createToken Jwts.builder()
                .setSubject(authentication.name)
                .claim(
                    Constants.AUTHORITIES_KEY,
                    authentication.authorities
                        .asSequence()
                        .map { it.authority }
                        .joinToString(separator = ","))
                .signWith(key, SignatureAlgorithm.HS512)
                .setExpiration(
                    if (rememberMe) Date(this + tokenValidityInMillisecondsForRememberMe)
                    else Date(this + tokenValidityInMilliseconds)
                )
                .serializeToJsonWith(JacksonSerializer())
                .compact()
        }
    }

    fun getAuthentication(token: String): Authentication {
        Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .body
            .apply {
                this[Constants.AUTHORITIES_KEY]
                    .toString()
                    .splitToSequence(",")
                    .mapTo(mutableListOf()) { SimpleGrantedAuthority(it) }
                    .apply authorities@{
                        return@getAuthentication UsernamePasswordAuthenticationToken(
                            User(subject, "", this@authorities),
                            token,
                            this@authorities
                        )
                    }
            }
    }

    fun validateToken(token: String): Boolean {
        try {
            Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
            return Constants.VALID_TOKEN
        } catch (e: JwtException) {
            Logging.i("Invalid Jwt token.")
            Logging.t("Invalid Jwt token trace. $e")
        } catch (e: IllegalArgumentException) {
            Logging.i("Invalid Jwt token.")
            Logging.t("Invalid Jwt token trace. $e")
        }
        return Constants.INVALID_TOKEN
    }
}