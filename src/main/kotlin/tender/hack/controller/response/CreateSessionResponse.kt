package tender.hack.controller.response

import java.time.OffsetDateTime
import java.util.UUID

class CreateSessionResponse(
    val sessionId: UUID,
    val createdAt: OffsetDateTime,
)