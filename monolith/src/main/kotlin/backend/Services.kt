@file:Suppress("unused")

package backend

import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service


@Service("userService")
//@Suppress("unused")
class UserService
    (
//    private val passwordEncoder: PasswordEncoder,
//    private val userRepository: UserRepository,
//    private val iUserRepository: IUserRepository,
//    private val userRepositoryPageable: UserRepositoryPageable,
//    private val userAuthRepository: UserAuthRepository,
//    private val authorityRepository: AuthorityRepository
    private val context: ApplicationContext,
) {
//    @PostConstruct
//    private fun init() = checkProfileLog(context)
//
//    @Transactional
//    suspend fun activateRegistration(key: String): User? =
//        log.debug("Activating user for activation key {}", key).run {
//            return@run iUserRepository.findOneByActivationKey(key).apply {
//                if (this != null) {
//                    activated = true
//                    activationKey = null
//                    saveUser(user = this).run {
//                        log.debug("Activated user: {}", this)
//                    }
//                } else log.debug("No user found with activation key {}", key)
//            }
//        }
//
//
//    suspend fun completePasswordReset(newPassword: String, key: String): User? =
//        log.debug("Reset user password for reset key {}", key).run {
//            userRepository.findOneByResetKey(key).apply {
//                return if (this != null &&
//                    resetDate?.isAfter(now().minusSeconds(86400)) == true
//                ) saveUser(
//                    apply {
//                        password = passwordEncoder.encode(newPassword)
//                        resetKey = null
//                        resetDate = null
//                    })
//                else null
//            }
//        }
//
//
//    @Transactional
//    suspend fun requestPasswordReset(mail: String): User? {
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
//
//    @Transactional
//    suspend fun register(account: Account, password: String): User? = userRepository
//        .findOneByLogin(account.login!!)
//        ?.apply isActivatedOnCheckLogin@{
//            if (!activated) return@isActivatedOnCheckLogin userRepository.delete(user = this)
//            else throw UsernameAlreadyUsedException()
//        }
//        .also {
//            userRepository.findOneByEmail(account.email!!)
//                ?.apply isActivatedOnCheckEmail@{
//                    if (!activated) return@isActivatedOnCheckEmail userRepository.delete(user = this)
//                    else throw EmailAlreadyUsedException()
//                }
//        }
//        .apply {
//            return@register userRepository.save(
//                User(
//                    login = account.login,
//                    password = passwordEncoder.encode(password),
//                    firstName = account.firstName,
//                    lastName = account.lastName,
//                    email = account.email,
//                    imageUrl = account.imageUrl,
//                    langKey = account.langKey,
//                    activated = USER_INITIAL_ACTIVATED_VALUE,
//                    activationKey = generateActivationKey,
//                    authorities = mutableSetOf<AuthorityEntity>().apply {
//                        add(AuthorityEntity(role = ROLE_USER))
//                    })
//            )
//        }
//
//    @Transactional
//    suspend fun createUser(account: Account): User =
//        saveUser(account.toUser().apply {
//            password = passwordEncoder.encode(generatePassword)
//            resetKey = generateResetKey
//            resetDate = now()
//            activated = true
//            account.authorities?.map {
//                authorities?.remove(AuthorityEntity(it))
//                authorityRepository.findById(it).apply auth@{
//                    if (this@auth != null) authorities!!.add(this@auth)
//                }
//            }
//        }).also {
//            log.debug("Created Information for User: {}", it)
//        }
//
//
//    /**
//     * Update all information for a specific user, and return the modified user.
//     *
//     * @param account user to update.
//     * @return updated user.
//     */
//    @Transactional
//    suspend fun updateUser(account: Account): Account =
//        if (account.id != null) account
//        else {
//            val user = iUserRepository.findById(account.id!!)
//            if (user == null) account
//            else Account(saveUser(user.apply {
//                login = account.login
//                firstName = account.firstName
//                lastName = account.lastName
//                email = account.email
//                imageUrl = account.imageUrl
//                activated = account.activated
//                langKey = account.langKey
//                if (!authorities.isNullOrEmpty()) {
//                    account.authorities!!.forEach {
//                        authorities?.remove(AuthorityEntity(it))
//                        authorityRepository.findById(it).apply auth@{
//                            if (this@auth != null) authorities!!.add(this@auth)
//                        }
//                    }
//                    authorities!!.clear()
//                    userAuthRepository.deleteAllUserAuthoritiesByUser(account.id!!)
//                }
//            }).also {
//                log.debug("Changed Information for User: {}", it)
//            })
//        }
//
//
//    @Transactional
//    suspend fun deleteUser(login: String): Unit =
//        userRepository.findOneByLogin(login).apply {
//            userRepository.delete(this!!)
//        }.run { log.debug("Changed Information for User: $this") }
//
//    /**
//     * Update basic information (first name, last name, email, language) for the current user.
//     *
//     * @param firstName first name of user.
//     * @param lastName  last name of user.
//     * @param email     email id of user.
//     * @param langKey   language key.
//     * @param imageUrl  image URL of user.
//     */
//    @Transactional
//    suspend fun updateUser(
//        firstName: String?,
//        lastName: String?,
//        email: String?,
//        langKey: String?,
//        imageUrl: String?
//    ): Unit = SecurityUtils.getCurrentUserLogin().run {
//        userRepository.findOneByLogin(login = this)?.apply {
//            this.firstName = firstName
//            this.lastName = lastName
//            this.email = email
//            this.langKey = langKey
//            this.imageUrl = imageUrl
//            saveUser(user = this).also {
//                log.debug("Changed Information for User: {}", it)
//            }
//        }
//    }
//
//
//    @Transactional
//    suspend fun saveUser(user: User): User = SecurityUtils.getCurrentUserLogin()
//        .run currentUserLogin@{
//            user.apply user@{
//                SYSTEM_USER.apply systemUser@{
//                    if (createdBy.isNullOrBlank()) {
//                        createdBy = this@systemUser
//                        lastModifiedBy = this@systemUser
//                    } else lastModifiedBy = this@currentUserLogin
//                }
//                userRepository.save(this@user)
//            }
//        }
//
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
//
//
//    @Transactional(readOnly = true)
//    suspend fun getAllManagedUsers(pageable: Pageable): Flow<Account> =
//        userRepositoryPageable
//            .findAllByIdNotNull(pageable)
//            .asFlow()
//            .map {
//                Account(
//                    userRepository.findOneWithAuthoritiesByLogin(it.login!!)!!
//                )
//            }
//
//
//    @Transactional(readOnly = true)
//    suspend fun getAvatars(pageable: Pageable)
//            : Flow<Avatar> = userRepositoryPageable
//        .findAllByActivatedIsTrue(pageable)
//        .filter { it != null }
//        .map { Avatar(it) }
//        .asFlow()
//
//    @Transactional(readOnly = true)
//    suspend fun countUsers(): Long = userRepository.count()
//
//    @Transactional(readOnly = true)
//    suspend fun getUserWithAuthoritiesByLogin(login: String): User? =
//        userRepository.findOneByLogin(login)
//
//    suspend fun findAccountByEmail(email: String): Account? =
//        Account(userRepository.findOneByEmail(email).apply {
//            if (this == null) return null
//        }!!)
//
//    suspend fun findAccountByLogin(login: String): Account? =
//        Account(userRepository.findOneWithAuthoritiesByLogin(login).apply {
//            if (this == null) return null
//        }!!)
//
//    /**
//     * Gets a list of all the authorities.
//     * @return a list of all the authorities.
//     */
//    @Transactional(readOnly = true)
//    suspend fun getAuthorities(): Flow<String> =
//        authorityRepository
//            .findAll()
//            .map { it.role }
//
//    @Transactional(readOnly = true)
//    suspend fun getUserWithAuthorities(): User? =
//        SecurityUtils.getCurrentUserLogin().run {
//            return@run if (isNullOrBlank()) null
//            else userRepository
//                .findOneWithAuthoritiesByLogin(this)
//        }
//
//    /**
//     * Not activated users should be automatically deleted after 3 days.
//     *
//     *
//     * This is scheduled to get fired everyday, at 01:00 (am).
//     */
//    @Scheduled(cron = "0 0 1 * * ?")
//    fun removeNotActivatedUsers() {
//        runBlocking {
//            removeNotActivatedUsersReactively()
//                .collect()
//        }
//    }
//
//    @Transactional
//    suspend fun removeNotActivatedUsersReactively(): Flow<User> = userRepository
//        .findAllByActivatedIsFalseAndActivationKeyIsNotNullAndCreatedDateBefore(
//            ofInstant(
//                now().minus(3, DAYS),
//                UTC
//            )
//        ).map {
//            it.apply {
//                userRepository.delete(this).also {
//                    log.debug("Deleted User: {}", this)
//                }
//            }
//        }
}

/*=================================================================================*/
