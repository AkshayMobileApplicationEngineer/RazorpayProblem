package build.point.paymentr

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity(), PaymentResultListener {

    private lateinit var amount: EditText
    private lateinit var buyNowButton: Button
    private val userId = "6859" // Declare userId as a class-level variable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        amount = findViewById(R.id.amount)
        buyNowButton = findViewById(R.id.buyNowButton)

        buyNowButton.setOnClickListener {
            val transactionAmount = "1"
            val transactionDescription = "test"
            paymentStart(userId, transactionAmount, transactionDescription)
        }
    }

    private fun paymentStart(userId: String, transactionAmount: String, transactionDescription: String) {
        val activity = this
        val co = Checkout()
        co.setKeyID("rzp_live_0e7KaXdVczrhK6")

        try {
            val options = JSONObject().apply {
                put("name", "Quick Payment")
                put("description", transactionDescription)
                put("image", "http://example.com/image/rzp.jpg")
                put("theme.color", "#3399cc")
                put("currency", "INR")
                put("amount", (transactionAmount.toDouble() * 100).toInt())
                put("retry", JSONObject().apply {
                    put("enabled", true)
                    put("max_count", 4)
                })
                put("prefill", JSONObject().apply {
                    put("name", "Akshay Kumar Prajapati") // Ideally, replace with actual user data
                    put("email", "akshaykumarprajapati@example.com") // Replace as necessary
                    put("contact", "8987918309") // Replace as necessary
                    put("userId", userId)
                })
                put("method", JSONObject().apply {
                    put("netbanking", true)
                    put("card", true)
                    put("wallet", true)
                    put("upi", true)
                })
            }

            co.open(activity, options)
        } catch (e: Exception) {
            Log.e("TAG", "paymentStart: ${e.message}")
            Toast.makeText(activity, "Error in payment: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    override fun onPaymentSuccess(response: String?) {
        Log.d("TAG", "onPaymentSuccess: $response")
        Toast.makeText(this, "Payment Successful", Toast.LENGTH_SHORT).show()
        handlePaymentSuccess(response)
    }

    override fun onPaymentError(code: Int, response: String?) {
        Log.e("TAG", "PaymentError: $response")
        Toast.makeText(this, "Payment Error: $response", Toast.LENGTH_SHORT).show()
        handlePaymentFailed()
    }

    private fun handlePaymentSuccess(response: String?) {
        val jsonResponse = response?.let { JSONObject(it) }
        val razorpayOrderId = jsonResponse?.getString("razorpay_order_id") ?: ""
        val razorpaySignature = jsonResponse?.getString("razorpay_signature") ?: ""

        sendTransactionToApi(
            userId = userId,
            transactionAmount = amount.text.toString().toDouble(),
            transactionType = "ADD", // or "DEDUCT"
            transactionStatus = "COMPLETED",
            transactionDescription = "Money Added To Wallet"
        )
        showDialog("Payment Successful", "Your payment was successful.")
    }

    private fun handlePaymentCancelled() {
        Log.d("TAG", "handlePaymentCancelled")
        Toast.makeText(this, "Payment Cancelled", Toast.LENGTH_SHORT).show()
        sendTransactionToApi(
            userId = userId,
            transactionAmount = amount.text.toString().toDouble(),
            transactionType = "ADD",
            transactionStatus = "PENDING",
            transactionDescription = "Payment Cancelled"
        )
        showDialog("Payment Cancelled", "Your payment was cancelled.")
    }

    private fun handlePaymentFailed() {
        Log.d("TAG", "handlePaymentFailed")
        Toast.makeText(this, "Payment Failed", Toast.LENGTH_SHORT).show()
        sendTransactionToApi(
            userId = userId,
            transactionAmount = amount.text.toString().toDouble(),
            transactionType = "ADD",
            transactionStatus = "DECLINED",
            transactionDescription = "Payment Failed"
        )
        showDialog("Payment Failed", "Your payment could not be processed. Please try again.")
    }

    private fun sendTransactionToApi(userId: String, transactionAmount: Double, transactionType: String, transactionStatus: String, transactionDescription: String) {
        Log.d("TAG", "sendTransactionToApi: $userId, $transactionAmount, $transactionType, $transactionStatus, $transactionDescription")
        val apiService = RetrofitInstance.apiService

        apiService.logTransaction(
            userId = userId.toInt(),
            transactionAmount = transactionAmount,
            transactionType = transactionType,
            transactionStatus = transactionStatus,
            transactionDescription = transactionDescription
        ).enqueue(object : Callback<TransactionResponse> {
            override fun onResponse(call: Call<TransactionResponse>, response: Response<TransactionResponse>) {
                Log.d("TAG", "onResponse: ${response.body()}")
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody?.status == "1") {
                        Toast.makeText(this@MainActivity, "Transaction logged successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@MainActivity, "Failed to log transaction: ${responseBody?.msg}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@MainActivity, "Failed to log transaction", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<TransactionResponse>, t: Throwable) {
                Log.e("TAG", "onFailure: ${t.message}")
                Toast.makeText(this@MainActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showDialog(title: String, message: String) {
        Log.d("TAG", "showDialog: $title, $message")
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }
        val dialog = builder.create()
        dialog.show()
    }
}