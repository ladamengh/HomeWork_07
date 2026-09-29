package otus.homework.customview

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val payload = resources.openRawResource(R.raw.payload).bufferedReader().use { reader ->
            reader.readText()
        }
        val spendings = Gson().fromJson(payload, Array<Spending>::class.java).toList()
        val amountByCategory = aggregateSpendings(spendings).associateBy(CategorySpending::category)

        val pieChartView = findViewById<SpendingPieChartView>(R.id.pieChartView)
        val totalTextView = findViewById<TextView>(R.id.totalTextView)
        val selectedCategoryTextView = findViewById<TextView>(R.id.selectedCategoryTextView)

        totalTextView.text = getString(R.string.full_sum, spendings.sumOf { it.amount.toLong() })
        pieChartView.setOnCategoryClickListener { category ->
            selectedCategoryTextView.text = if (category == null) {
                getString(R.string.select_category_hint)
            } else {
                val amount = amountByCategory.getValue(category).amount
                getString(R.string.selected_category, category, amount)
            }
        }
        pieChartView.setData(spendings)
    }
}

data class Spending(
    val id: Int,
    val name: String,
    val amount: Int,
    val category: String,
    val time: Long,
)
