package otus.homework.customview

internal data class CategorySpending(
    val category: String,
    val amount: Long,
)

internal fun aggregateSpendings(spendings: List<Spending>): List<CategorySpending> {
    val amountsByCategory = linkedMapOf<String, Long>()

    spendings.forEach { spending ->
        if (spending.amount > 0) {
            amountsByCategory[spending.category] =
                amountsByCategory.getOrDefault(spending.category, 0L) + spending.amount
        }
    }

    return amountsByCategory.map { (category, amount) ->
        CategorySpending(category, amount)
    }
}

internal fun sweepAngle(amount: Long, totalAmount: Long): Float {
    if (amount <= 0L || totalAmount <= 0L) return 0f
    return amount.toDouble().div(totalAmount).times(360.0).toFloat()
}

internal fun categoryAtAngle(
    categories: List<CategorySpending>,
    totalAmount: Long,
    angle: Float,
): String? {
    if (categories.isEmpty() || totalAmount <= 0L || angle !in 0f..360f) return null

    var endAngle = 0f
    categories.forEachIndexed { index, categorySpending ->
        endAngle += sweepAngle(categorySpending.amount, totalAmount)
        if (angle < endAngle || index == categories.lastIndex) {
            return categorySpending.category
        }
    }

    return null
}
