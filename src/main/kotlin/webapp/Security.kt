@file:Suppress("unused")

package webapp


import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.Jwts.parserBuilder
import io.jsonwebtoken.SignatureAlgorithm.HS512
import io.jsonwebtoken.io.Decoders.BASE64
import io.jsonwebtoken.jackson.io.JacksonSerializer
import io.jsonwebtoken.lang.Strings.hasLength
import io.jsonwebtoken.security.Keys.hmacShaKeyFor
import org.springframework.beans.factory.InitializingBean
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component
import webapp.Constants.AUTHORITIES_KEY
import webapp.Constants.INVALID_TOKEN
import webapp.Constants.VALID_TOKEN
import webapp.Logging.d
import webapp.Logging.i
import webapp.Logging.t
import webapp.Logging.w
import java.security.Key
import java.util.*
import kotlin.text.Charsets.UTF_8
import org.springframework.security.core.userdetails.User as UserSecurity


/*=================================================================================*/

@Component
class Security(
    private val properties: Properties
) : InitializingBean {

    private var key: Key? = null
    private var tokenValidityInMilliseconds: Long = 0
    private var tokenValidityInMillisecondsForRememberMe: Long = 0

    @Throws(Exception::class)
    override fun afterPropertiesSet() {
        properties
            .security
            .authentication
            .jwt
            .secret
            .run {
                key = hmacShaKeyFor(
                    when {
                        !hasLength(this) -> w(
                            "Warning: the Jwt key used is not Base64-encoded. " +
                                    "We recommend using the `webapp.security.authentication.jwt.base64-secret`" +
                                    " key for optimum security."
                        ).run { toByteArray(UTF_8) }

                        else -> d("Using a Base64-encoded Jwt secret key").run {
                            BASE64.decode(
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
                    AUTHORITIES_KEY,
                    authentication.authorities
                        .asSequence()
                        .map { it.authority }
                        .joinToString(separator = ","))
                .signWith(key, HS512)
                .setExpiration(
                    if (rememberMe) Date(this + tokenValidityInMillisecondsForRememberMe)
                    else Date(this + tokenValidityInMilliseconds)
                )
                .serializeToJsonWith(JacksonSerializer())
                .compact()
        }
    }

    fun getAuthentication(token: String): Authentication {
        parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .body
            .apply {
                this[AUTHORITIES_KEY]
                    .toString()
                    .splitToSequence(",")
                    .mapTo(mutableListOf()) { SimpleGrantedAuthority(it) }
                    .apply authorities@{
                        return@getAuthentication UsernamePasswordAuthenticationToken(
                            UserSecurity(subject, "", this@authorities),
                            token,
                            this@authorities
                        )
                    }
            }
    }

    fun validateToken(token: String): Boolean {
        try {
            parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
            return VALID_TOKEN
        } catch (e: JwtException) {
            i("Invalid Jwt token.")
            t("Invalid Jwt token trace. $e")
        } catch (e: IllegalArgumentException) {
            i("Invalid Jwt token.")
            t("Invalid Jwt token trace. $e")
        }
        return INVALID_TOKEN
    }
//TODO: put config as inner class

}

/*=================================================================================*/

/*=================================================================================*/