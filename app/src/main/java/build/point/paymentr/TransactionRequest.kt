// TransactionRequest.kt
package build.point.paymentr

data class TransactionRequest(
    val user_id: Int,
    val transaction_amount: Double,
    val transaction_type: String,
    val transaction_status: String,
    val transaction_description: String
)
