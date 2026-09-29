package otus.homework.customview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.toColorInt
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class SpendingPieChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private val colors = listOf(
        "#F06292",
        "#BA68C8",
        "#64B5F6",
        "#4DB6AC",
        "#81C784",
        "#FFD54F",
        "#FF8A65",
        "#A1887F",
        "#90A4AE",
        "#9575CD",
        "#4FC3F7",
        "#AED581",
        "#FF7043",
        "#DCE775",
        "#FDD835",
    ).map(String::toColorInt)

    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.BUTT
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GRAY
        textSize = spToPx(CENTER_TEXT_SIZE_SP)
        textAlign = Paint.Align.CENTER
    }

    private val chartBounds = RectF()
    private val baseStrokeWidth = dpToPx(STROKE_WIDTH_DP).toFloat()
    private val selectedStrokeWidth = baseStrokeWidth + dpToPx(SELECTED_STROKE_EXTRA_DP)

    private var categories: List<CategorySpending> = emptyList()
    private var totalAmount = 0L
    private var centerX = 0f
    private var centerY = 0f
    private var radius = 0f
    private var touchStartedInView = false

    private var selectedCategory: String? = null
    private var onCategoryClick: ((String?) -> Unit)? = null

    fun setData(spendings: List<Spending>) {
        categories = aggregateSpendings(spendings)
        totalAmount = categories.sumOf(CategorySpending::amount)

        if (categories.none { it.category == selectedCategory }) {
            selectedCategory = null
        }

        contentDescription = context.getString(R.string.pie_chart_content_description)
        invalidate()
    }

    fun setOnCategoryClickListener(listener: ((String?) -> Unit)?) {
        onCategoryClick = listener
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredContentSize = dpToPx(DEFAULT_SIZE_DP)
        val desiredWidth = max(suggestedMinimumWidth, desiredContentSize + paddingLeft + paddingRight)
        val desiredHeight = max(suggestedMinimumHeight, desiredContentSize + paddingTop + paddingBottom)

        val measuredWidth = resolveSizeAndState(desiredWidth, widthMeasureSpec, 0)
        val measuredHeight = resolveSizeAndState(desiredHeight, heightMeasureSpec, 0)
        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        val contentWidth = (w - paddingLeft - paddingRight).coerceAtLeast(0)
        val contentHeight = (h - paddingTop - paddingBottom).coerceAtLeast(0)

        centerX = paddingLeft + contentWidth / 2f
        centerY = paddingTop + contentHeight / 2f
        radius = (min(contentWidth, contentHeight) - selectedStrokeWidth) / 2f
        radius = radius.coerceAtLeast(0f)

        chartBounds.set(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius,
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (categories.isEmpty() || totalAmount <= 0L || radius <= 0f) {
            return
        }

        var startAngle = START_ANGLE
        categories.forEachIndexed { index, categorySpending ->
            val sweepAngle = sweepAngle(categorySpending.amount, totalAmount)
            arcPaint.color = colors[index % colors.size]
            arcPaint.strokeWidth = if (categorySpending.category == selectedCategory) {
                selectedStrokeWidth
            } else {
                baseStrokeWidth
            }

            canvas.drawArc(chartBounds, startAngle, sweepAngle, false, arcPaint)
            startAngle += sweepAngle
        }

        drawCenterText(canvas)
    }

    private fun drawCenterText(canvas: Canvas) {
        val text = selectedCategory ?: context.getString(R.string.tap_to_see_more)
        val baseline = centerY - (textPaint.ascent() + textPaint.descent()) / 2f
        canvas.drawText(text, centerX, baseline, textPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled || categories.isEmpty()) {
            return false
        }

        return when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchStartedInView = true
                if (isPointOnChart(event.x, event.y)) {
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
                true
            }

            MotionEvent.ACTION_UP -> {
                val shouldHandleClick = touchStartedInView
                touchStartedInView = false
                parent?.requestDisallowInterceptTouchEvent(false)

                if (shouldHandleClick) {
                    if (isPointOnChart(event.x, event.y)) {
                        selectCategoryAt(event.x, event.y)
                    } else {
                        clearSelection()
                    }
                    performClick()
                }
                shouldHandleClick
            }

            MotionEvent.ACTION_CANCEL -> {
                touchStartedInView = false
                parent?.requestDisallowInterceptTouchEvent(false)
                false
            }

            else -> touchStartedInView
        }
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun isPointOnChart(x: Float, y: Float): Boolean {
        val dx = x - centerX
        val dy = y - centerY
        val distance = sqrt(dx * dx + dy * dy)
        val innerRadius = (radius - selectedStrokeWidth / 2f).coerceAtLeast(0f)
        val outerRadius = radius + selectedStrokeWidth / 2f
        return distance in innerRadius..outerRadius
    }

    private fun selectCategoryAt(x: Float, y: Float) {
        val rawAngle = Math.toDegrees(atan2(y - centerY, x - centerX).toDouble()).toFloat()
        val angleFromTopClockwise = (rawAngle - START_ANGLE + FULL_CIRCLE) % FULL_CIRCLE
        val category = categoryAtAngle(categories, totalAmount, angleFromTopClockwise) ?: return

        selectedCategory = category
        onCategoryClick?.invoke(category)
        invalidate()
    }

    private fun clearSelection() {
        selectedCategory = null
        onCategoryClick?.invoke(null)
        invalidate()
    }

    override fun onSaveInstanceState(): Parcelable {
        return SavedState(super.onSaveInstanceState(), selectedCategory)
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state !is SavedState) {
            super.onRestoreInstanceState(state)
            return
        }

        super.onRestoreInstanceState(state.superState)
        selectedCategory = state.selectedCategory
        invalidate()
    }

    private class SavedState : BaseSavedState {
        val selectedCategory: String?

        constructor(superState: Parcelable?, selectedCategory: String?) : super(superState) {
            this.selectedCategory = selectedCategory
        }

        private constructor(parcel: Parcel) : super(parcel) {
            selectedCategory = parcel.readString()
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeString(selectedCategory)
        }

        companion object CREATOR : Parcelable.Creator<SavedState> {
            override fun createFromParcel(parcel: Parcel): SavedState = SavedState(parcel)

            override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)
        }
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    private fun spToPx(sp: Int): Float = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        sp.toFloat(),
        resources.displayMetrics,
    )

    private companion object {
        const val DEFAULT_SIZE_DP = 240
        const val STROKE_WIDTH_DP = 48
        const val SELECTED_STROKE_EXTRA_DP = 8
        const val CENTER_TEXT_SIZE_SP = 14
        const val START_ANGLE = -90f
        const val FULL_CIRCLE = 360f
    }
}
