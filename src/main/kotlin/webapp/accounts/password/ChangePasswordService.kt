package webapp.accounts.password

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class ChangePasswordService {
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

}

