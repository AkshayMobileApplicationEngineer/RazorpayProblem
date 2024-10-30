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
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity(), PaymentResultListener {

    private lateinit var amount: EditText
    private lateinit var paynow: Button
    private lateinit var buyNowButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        amount = findViewById(R.id.amount)
        paynow = findViewById(R.id.paynow)
        buyNowButton = findViewById(R.id.buyNowButton)

        paynow.setOnClickListener {
            val amountText = amount.text.toString()
            if (amountText.isNotEmpty() && amountText.toDoubleOrNull() != null) {
                startPayment(amountText)
            } else {
                Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            }
        }

        buyNowButton.setOnClickListener {
            val userId = "1234"
            val transitionAmount = "1"
            val transitionDescription = "test"
            paymentStart(userId, transitionAmount, transitionDescription)
        }
    }

    private fun startPayment(amountText: String) {
        val userId = "1234" // Retrieve this dynamically in a real app
        paymentStart(userId, amountText, "Payment for $amountText")
    }

    private fun paymentStart(userId: String, transitionAmount: String, transitionDescription: String) {
        val activity = this
        val co = Checkout()
        co.setKeyID("rzp_live_0e7KaXdVczrhK6")

        try {
            val options = JSONObject().apply {
                put("name", "Razorpay Corp")
                put("description", transitionDescription)
                put("image", "http://example.com/image/rzp.jpg")
                put("theme.color", "#3399cc")
                put("currency", "INR")
                put("amount", (transitionAmount.toDouble() * 100).toInt())
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Checkout.RZP_REQUEST_CODE) {
            when (resultCode) {
                RESULT_OK -> {
                    val response = data?.getStringExtra("response")
                    handlePaymentSuccess(response)
                }
                RESULT_CANCELED -> {
                    handlePaymentCancelled()
                }
                else -> {
                    handlePaymentFailed()
                }
            }
        }
    }

    private fun handlePaymentSuccess(response: String?) {
        val jsonResponse = JSONObject(response)
        val razorpayOrderId = jsonResponse.getString("razorpay_order_id")
        val razorpaySignature = jsonResponse.getString("razorpay_signature")

        // Log the transaction
        sendTransactionToApi(
            userId = "1234", // Use the actual user ID
            transactionAmount = amount.text.toString().toDouble(),
            transactionType = "ADD", // or "DEDUCT"
            transactionStatus = "COMPLETED",
            transactionDescription = "Money Added To Wallet"
        )
        dialogPaymentSuccess()
        Toast.makeText(this, "Payment Successful", Toast.LENGTH_SHORT).show()
    }

    private fun dialogPaymentSuccess() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Payment Successful")
        builder.setMessage("Your payment was successful.")
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }
        val dialog = builder.create()
        dialog.show()
    }

    private fun handlePaymentCancelled() {
        val userId = "1234" // Use the actual user ID
        val transactionAmount = amount.text.toString().toDouble()

        // Log the transaction
        sendTransactionToApi(
            userId = userId,
            transactionAmount = transactionAmount,
            transactionType = "ADD", // or "DEDUCT"
            transactionStatus = "PENDING",
            transactionDescription = "Payment Cancelled"
        )
        paymentCaneclledDialog()
        Toast.makeText(this, "Payment Cancelled", Toast.LENGTH_SHORT).show()
    }

    private fun paymentCaneclledDialog() {
        //TODO("Not yet implemented")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Payment Cancelled")
        builder.setMessage("Your payment was cancelled.")
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }
        val dialog = builder.create()
        dialog.show()
    }

    private fun handlePaymentFailed() {
        val userId = "1234" // Use the actual user ID
        val transactionAmount = amount.text.toString().toDouble()

        // Log the transaction
        sendTransactionToApi(
            userId = userId,
            transactionAmount = transactionAmount,
            transactionType = "ADD", // or "DEDUCT"
            transactionStatus = "DECLINED",
            transactionDescription = "Payment Failed"
        )

        Toast.makeText(this, "Payment Failed", Toast.LENGTH_SHORT).show()
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Payment Failed")
        builder.setMessage("Your payment could not be processed. Please try again.")
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }
        val dialog = builder.create()
        dialog.show()
    }

    override fun onPaymentSuccess(p0: String?) {
        handlePaymentSuccess(p0)
    }

    override fun onPaymentError(p0: Int, p1: String?) {
        Toast.makeText(this, "Payment Error: $p1", Toast.LENGTH_SHORT).show()
        handlePaymentFailed()
    }

    private fun sendTransactionToApi(userId: String, transactionAmount: Double, transactionType: String, transactionStatus: String, transactionDescription: String) {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://merabihar.in/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(ApiService::class.java)

        apiService.logTransaction(
            userId = userId.toInt(),
            transactionAmount = transactionAmount,
            transactionType = transactionType,
            transactionStatus = transactionStatus,
            transactionDescription = transactionDescription
        ).enqueue(object : Callback<payapi> {
            override fun onResponse(call: Call<payapi>, response: Response<payapi>) {
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody?.status == "1") {
                        Toast.makeText(this@MainActivity, "Transaction logged successfully", Toast.LENGTH_SHORT).show()
                        Log.d("TAG", "Transaction logged successfully")
                        transactionloggedsuccessfully()
                    } else {
                        Toast.makeText(this@MainActivity, "Failed to log transaction: ${responseBody?.msg}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@MainActivity, "Failed to log transaction", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<payapi>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun transactionloggedsuccessfully() {
        //TODO("Not yet implemented")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Transaction Logged Successfully")
        builder.setMessage("Your transaction was logged successfully.")
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }
        val dialog = builder.create()
        dialog.show()
    }
}
