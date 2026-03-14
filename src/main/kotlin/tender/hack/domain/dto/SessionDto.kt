package tender.hack.domain.dto

class SessionDto(
    val userDto: UserDto,
    val items: List<SessionItemDto>
) {
}