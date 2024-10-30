package build.point.paymentr

data class Data(
    val transaction_id: Int
)

data class TransactionResponse(
    val data: Data?,
    val msg: String,
    val status: String
)
