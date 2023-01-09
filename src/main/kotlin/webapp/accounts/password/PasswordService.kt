package webapp.accounts.password

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import webapp.Bootstrap.log
import webapp.accounts.models.AccountCredentials
import webapp.accounts.repository.AccountRepository
import java.time.Instant.now

@Service
@Transactional
class PasswordService(private val accountRepository: AccountRepository) {
    fun changePassword(currentPassword: String, newPassword: String) {
        TODO("Not yet implemented")
    }

    //    @Transactional
//    suspend fun changePassword(currentClearTextPassword: String, newPassword: String) {
//        SecurityUtils.getCurrentUserLogin().apply {
//            if (!isNullOrBlank()) {
//                userRepository.findOneByLogin(this).apply {
//                    if (this != null) {
//                        if (!passwordEncoder.matches(
//                                currentClearTextPassword,
//                                password
//                            )
//                        ) throw InvalidPasswordException()
//                        else saveUser(this.apply {
//                            password = passwordEncoder.encode(newPassword)
//                        }).run {
//                            log.debug("Changed password for User: {}", this)
//                        }
//                    }
//                }
//            }
//        }
//    }
    suspend fun completePasswordReset(newPassword: String, key: String): AccountCredentials? =
        accountRepository.findOneByResetKey(key).run {
            if (this != null && resetDate?.isAfter(now().minusSeconds(86400)) == true) {
                log.debug("Reset account password for reset key $key")
                return@completePasswordReset toCredentialsModel
                //                return saveUser(
                //                apply {
                ////                    password = passwordEncoder.encode(newPassword)
                //                    resetKey = null
                //                    resetDate = null
                //                })
            } else {
                log.debug("$key is not a valid reset account password key")
                return@completePasswordReset null
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

