package tender.hack.controller.response

import tender.hack.domain.dto.SessionDto
import java.time.OffsetDateTime

class GetSessionResponse(
    val id: String,
    val createdAt: OffsetDateTime,
    val session: Session
)