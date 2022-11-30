@file:Suppress("unused")

package backend

class ChangePasswordControllerTests {

/*
    @Test
    @WithMockUser("change-password-wrong-existing-password")
    fun testChangePasswordWrongExistingPassword() {
        val currentPassword = RandomStringUtils.random(60)
        val user = User(
            password = passwordEncoder.encode(currentPassword),
            login = "change-password-wrong-existing-password",
            createdBy = SYSTEM_ACCOUNT,
            email = "change-password-wrong-existing-password@example.com"
        )

        userRepository.save(user).block()

        accountWebTestClient.post().uri("/api/account/change-password")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(convertObjectToJsonBytes(PasswordChangeDTO("1$currentPassword", "new password")))
            .exchange()
            .expectStatus().isBadRequest

        val updatedUser = userRepository.findOneByLogin("change-password-wrong-existing-password").block()
        assertThat(passwordEncoder.matches("new password", updatedUser.password)).isFalse
        assertThat(passwordEncoder.matches(currentPassword, updatedUser.password)).isTrue
    }

    @Test
    @WithMockUser("change-password")
    fun testChangePassword() {
        val currentPassword = RandomStringUtils.random(60)
        val user = User(
            password = passwordEncoder.encode(currentPassword),
            login = "change-password",
            createdBy = SYSTEM_ACCOUNT,
            email = "change-password@example.com"
        )

        userRepository.save(user).block()

        accountWebTestClient.post().uri("/api/account/change-password")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(convertObjectToJsonBytes(PasswordChangeDTO(currentPassword, "new password")))
            .exchange()
            .expectStatus().isOk

        val updatedUser = userRepository.findOneByLogin("change-password").block()
        assertThat(passwordEncoder.matches("new password", updatedUser.password)).isTrue
    }

    @Test
    @WithMockUser("change-password-too-small")
    fun testChangePasswordTooSmall() {
        val currentPassword = RandomStringUtils.random(60)
        val user = User(
            password = passwordEncoder.encode(currentPassword),
            login = "change-password-too-small",
            createdBy = SYSTEM_ACCOUNT,
            email = "change-password-too-small@example.com"
        )

        userRepository.save(user).block()

        val newPassword = RandomStringUtils.random(ManagedUserVM.PASSWORD_MIN_LENGTH - 1)

        accountWebTestClient.post().uri("/api/account/change-password")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(convertObjectToJsonBytes(PasswordChangeDTO(currentPassword, newPassword)))
            .exchange()
            .expectStatus().isBadRequest

        val updatedUser = userRepository.findOneByLogin("change-password-too-small").block()
        assertThat(updatedUser.password).isEqualTo(user.password)
    }

    @Test
    @WithMockUser("change-password-too-long")
    fun testChangePasswordTooLong() {
        val currentPassword = RandomStringUtils.random(60)
        val user = User(
            password = passwordEncoder.encode(currentPassword),
            login = "change-password-too-long",
            createdBy = SYSTEM_ACCOUNT,
            email = "change-password-too-long@example.com"
        )

        userRepository.save(user).block()

        val newPassword = RandomStringUtils.random(ManagedUserVM.PASSWORD_MAX_LENGTH + 1)

        accountWebTestClient.post().uri("/api/account/change-password")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(convertObjectToJsonBytes(PasswordChangeDTO(currentPassword, newPassword)))
            .exchange()
            .expectStatus().isBadRequest

        val updatedUser = userRepository.findOneByLogin("change-password-too-long").block()
        assertThat(updatedUser.password).isEqualTo(user.password)
    }

    @Test
    @WithMockUser("change-password-empty")
    fun testChangePasswordEmpty() {
        val currentPassword = RandomStringUtils.random(60)
        val user = User(
            password = passwordEncoder.encode(currentPassword),
            login = "change-password-empty",
            createdBy = SYSTEM_ACCOUNT,
            email = "change-password-empty@example.com"
        )

        userRepository.save(user).block()

        accountWebTestClient.post().uri("/api/account/change-password")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(convertObjectToJsonBytes(PasswordChangeDTO(currentPassword, "")))
            .exchange()
            .expectStatus().isBadRequest

        val updatedUser = userRepository.findOneByLogin("change-password-empty").block()
        assertThat(updatedUser.password).isEqualTo(user.password)
    }
 */

}