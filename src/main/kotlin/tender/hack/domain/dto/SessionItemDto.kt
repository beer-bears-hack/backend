package tender.hack.domain.dto

class SessionItemDto(
    val itemId: String,
    val steId: String,
    val name: String,
    val quantity: Int,
    val unitPrice: Double,
    val totalPrice: Double,
) {
}