package webapp.models.entities

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import webapp.Constants
import java.util.*

/*=================================================================================*/
@Table("`phone`")
data class PhoneEntity(
    @Id var id: UUID? = null,
    @field:NotNull
    @field:Pattern(regexp = Constants.LOGIN_REGEX)
    @field:Size(min = 1, max = 50)
    var value: String? = null
)