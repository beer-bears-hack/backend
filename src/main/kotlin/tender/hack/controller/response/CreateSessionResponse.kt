package tender.hack.controller.response

import java.time.OffsetDateTime
import java.util.UUID

class CreateSessionResponse(
    val sessionId: String,
    val createdAt: OffsetDateTime,
)