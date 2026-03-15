package tender.hack.domain.dto

class SessionItemDto(
    val name: String,
    val category: String?,
    val quantity: Int,
    val unitPrice: Double,
    val totalPrice: Double,
    val resultId: String
) {
}