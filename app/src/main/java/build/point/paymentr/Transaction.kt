
package build.point.paymentr

data class Transaction(
    val user_id: Int,
    val transaction_amount: Double,
    val transaction_type: String,
    val transaction_status: String,
    val transaction_description: String
)
