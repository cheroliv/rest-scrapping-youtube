package webapp.accounts.models.entities

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

/*=================================================================================*/
@Table("`authority`")
data class AuthorityEntity(
    @Id
    @field:NotNull
    @field:Size(max = 50)
    @Column(AuthorityRecord.ROLE_COLUMN)
    override val role: String
) : AuthorityRecord