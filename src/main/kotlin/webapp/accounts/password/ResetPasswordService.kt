package webapp.accounts.password

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import webapp.Bootstrap
import webapp.accounts.models.AccountCredentials
import webapp.accounts.repository.AccountRepository
import java.time.Instant

@Service
@Transactional
class ResetPasswordService(
    private val accountRepository: AccountRepository
) {
    suspend fun completePasswordReset(newPassword: String, key: String): AccountCredentials? =
        accountRepository.findOneByResetKey(key).run {
            when {
                this != null && resetDate?.isAfter(
                    Instant.now().minusSeconds(86400)
                ) == true -> {
                    Bootstrap.log.debug("Reset account password for reset key $key")
                    return@completePasswordReset toCredentialsModel
                    //                return saveUser(
                    //                apply {
                    ////                    password = passwordEncoder.encode(newPassword)
                    //                    resetKey = null
                    //                    resetDate = null
                    //                })
                }

                else -> {
                    Bootstrap.log.debug("$key is not a valid reset account password key")
                    return@completePasswordReset null
                }
            }
        }


    suspend fun requestPasswordReset(mail: String): AccountCredentials? = null
//        return userRepository
//            .findOneByEmail(mail)
//            .apply {
//                if (this != null && this.activated) {
//                    resetKey = generateResetKey
//                    resetDate = now()
//                    saveUser(this)
//                } else return null
//            }
//    }

}