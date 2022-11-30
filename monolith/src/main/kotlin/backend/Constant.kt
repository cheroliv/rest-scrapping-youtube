@file:Suppress("unused")

package backend

import java.net.URI
import java.net.URI.create


object Constants {

    //SignupController
    @JvmStatic
    val ALLOWED_ORDERED_PROPERTIES = arrayOf(
        "id",
        "login",
        "firstName",
        "lastName",
        "email",
        "activated",
        "langKey"
    )
    const val NORMAL_TERMINATION = 0

    const val DOMAIN_DEV_URL = "acme.com"
    const val DOMAIN_URL = "https://www.cheroliv.com"
    const val STARTUP_HOST_WARN_LOG_MSG = "The host name could not be determined, using `localhost` as fallback"
    const val SPRING_APPLICATION_NAME = "spring.application.name"
    const val SERVER_SSL_KEY_STORE = "server.ssl.key-store"
    const val SERVER_PORT = "server.port"
    const val SERVER_SERVLET_CONTEXT_PATH = "server.servlet.context-path"
    const val EMPTY_CONTEXT_PATH = "/"
    const val HTTPS = "https"
    const val HTTP = "http"
    const val PROFILE_SEPARATOR = ","

    //Spring profiles
    const val SPRING_PROFILE_DEVELOPMENT = "dev"
    const val SPRING_PROFILE_PRODUCTION = "prod"
    const val SPRING_PROFILE_CLOUD = "cloud"
    const val SPRING_PROFILE_CONF_DEFAULT_KEY = "spring.profiles.default"
    const val SPRING_PROFILE_TEST = "test"
    const val SPRING_PROFILE_HEROKU = "heroku"
    const val SPRING_PROFILE_AWS_ECS = "aws-ecs"
    const val SPRING_PROFILE_AZURE = "azure"
    const val SPRING_PROFILE_SWAGGER = "swagger"
    const val SPRING_PROFILE_NO_LIQUIBASE = "no-liquibase"
    const val SPRING_PROFILE_K8S = "k8s"
    const val PROFILE_CLI = "cli"
    val PROFILE_CLI_PROPS = mapOf("spring.main.web-application-type" to "none")

    //Config
    const val DEV_HOST = "localhost"

    //HTTP param
    const val REQUEST_PARAM_LANG = "lang"
    const val CONTENT_SECURITY_POLICY =
        "default-src 'self'; frame-src 'self' data:; script-src 'self' 'unsafe-inline' 'unsafe-eval' https://storage.googleapis.com; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self' data:"
    const val FEATURE_POLICY =
        "geolocation 'none'; midi 'none'; sync-xhr 'none'; microphone 'none'; camera 'none'; magnetometer 'none'; gyroscope 'none'; speaker 'none'; fullscreen 'self'; payment 'none'"

    //Security
    const val ROLE_ADMIN = "ADMIN"
    const val ROLE_USER = "USER"
    const val ROLE_ANONYMOUS = "ANONYMOUS"
    const val AUTHORITIES_KEY = "auth"
    const val AUTHORIZATION_HEADER = "Authorization"
    const val BEARER_START_WITH = "Bearer "
    const val AUTHORIZATION_ID_TOKEN = "id_token"
    const val VALID_TOKEN = true
    const val INVALID_TOKEN = false

    //REST API
    //URIs

    const val AUTHORITY_API = "/api/authorities"
    const val ACCOUNT_API = "/api/account"
    const val SIGNUP_API = "/signup"
    const val SIGNUP_API_PATH = "$ACCOUNT_API$SIGNUP_API"
    const val ACTIVATE_API = "/activate"
    const val ACTIVATE_API_PATH = "$ACCOUNT_API$ACTIVATE_API?key="
    const val ACTIVATE_API_PARAM = "{activationKey}"
    const val ACTIVATE_API_KEY = "key"
    const val RESET_PASSWORD_API_INIT = "/reset-password/init"
    const val RESET_PASSWORD_API_FINISH = "/reset-password/finish"
    const val CHANGE_PASSWORD_API = "/change-password"

    //properties
    const val PROP_ITEM = "backend.item"
    const val PROP_MESSAGE = "backend.message"
    const val PROP_MAIL_BASE_URL = "backend.mail.base-url"
    const val PROP_MAIL_FROM = "backend.mail.from"
    const val PROP_MAIL_HOST = "backend.mail.host"
    const val PROP_MAIL_PORT = "backend.mail.port"
    const val PROP_MAIL_PASSWORD = "backend.mail.password"
    const val PROP_MAIL_PROPERTY_DEBUG = "backend.mail.property.debug"
    const val PROP_MAIL_PROPERTY_TRANSPORT_PROTOCOL = "backend.mail.property.transport.protocol"
    const val PROP_MAIL_PROPERTY_SMTP_AUTH = "backend.mail.property.smtp.auth"
    const val PROP_MAIL_PROPERTY_SMTP_STARTTLS_ENABLE = "backend.mail.property.smtp.starttls.enable"
    const val PROP_DATABASE_POPULATOR_PATH = "backend.database.populator-path"
    const val STARTUP_LOG_MSG_KEY = "startup.log.msg"


    //Email activation
    const val USER = "user"
    const val ADMIN = "admin"
    const val BASE_URL = "baseUrl"

    // Regex for acceptable logins
    const val LOGIN_REGEX =
        "^(?>[a-zA-Z0-9!$&*+=?^_`{|}~.-]+@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*)|(?>[_.@A-Za-z0-9-]+)$"
    const val SYSTEM_USER = "system"


    @Suppress("SpellCheckingInspection")
    const val ANONYMOUS_USER: String = "anonymoususer"
    const val DEFAULT_LANGUAGE = "en"
    const val PASSWORD_MIN_LENGTH: Int = 4
    const val PASSWORD_MAX_LENGTH: Int = 100
    const val ERR_CONCURRENCY_FAILURE: String = "error.concurrencyFailure"
    const val ERR_VALIDATION: String = "error.validation"
    const val USER_INITIAL_ACTIVATED_VALUE = false
    const val IMAGE_URL_DEFAULT = "http://placehold.it/50x50"
    private const val PROBLEM_BASE_URL: String = "$DOMAIN_URL/problem"


    @JvmField
    val DEFAULT_TYPE: URI = create("$PROBLEM_BASE_URL/problem-with-message")

    @JvmField
    val CONSTRAINT_VIOLATION_TYPE: URI = create("$PROBLEM_BASE_URL/constraint-violation")

    @JvmField
    val INVALID_PASSWORD_TYPE: URI = create("$PROBLEM_BASE_URL/invalid-password")

    @JvmField
    val EMAIL_ALREADY_USED_TYPE: URI = create("$PROBLEM_BASE_URL/email-already-used")

    @JvmField
    val LOGIN_ALREADY_USED_TYPE: URI = create("$PROBLEM_BASE_URL/login-already-used")
}