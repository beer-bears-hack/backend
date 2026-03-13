package tender.hack.domain.dto

import java.util.UUID
import java.time.OffsetDateTime

class UserDto(
    val uuid: UUID,
    val created: OffsetDateTime
)