package webapp.signup

import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import webapp.Constants.ROLE_USER
import webapp.accounts.entities.AccountAuthorityEntity
import webapp.accounts.entities.AccountEntity
import webapp.accounts.models.AccountCredentials

@Service
@Transactional
class SignupService(
    private val dao: R2dbcEntityTemplate,
) {
     suspend fun signup(accountCredentials: AccountCredentials) {
        dao.insert(AccountEntity(accountCredentials))
            .awaitSingleOrNull()
            ?.id
            .run {
                if (this != null) dao.insert(
                    AccountAuthorityEntity(
                        userId = this,
                        role = ROLE_USER
                    )
                ).awaitSingleOrNull()
            }
    }
}