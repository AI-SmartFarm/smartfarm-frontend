package com.smartfarm.app.ui.common

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.ColorUtils
import com.smartfarm.app.R

/**
 * 외부 라이브러리 없이 직접 그리는 꺾은선 그래프.
 *
 * ▸ 모양(색, 눈금, 적정 범위 띠)은 XML 속성으로 지정  → res/values/attrs.xml 참고
 *     app:chartLineColor  app:chartAreaColor  app:chartYMin  app:chartYMax  app:chartYStep
 *     app:chartNormalMin  app:chartNormalMax
 * ▸ 숫자는 코드에서 넣기
 *     binding.chart.setData(labels, values)
 *
 * 실제 데이터는 24시간 × 30분 = 점 최대 48개라서
 *  - 선은 부드러운 곡선으로, 점(동그라미)은 마지막 값(= 지금)만 강조
 *  - x축 시간 글자는 겹치지 않게 최대 5개만, 같은 시각은 한 번만 표시
 * 점이 적을 때(12개 이하, 미리보기 등)는 모든 점에 동그라미를 그립니다.
 */
class SimpleLineChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        /** 점이 이 개수 이하일 때만 모든 점에 동그라미 */
        private const val MAX_POINTS_WITH_DOTS = 12
        /** x축 시간 글자 최대 개수 */
        private const val MAX_X_LABELS = 5
    }

    // ----- XML 속성으로 정하는 모양 -----
    private var lineColor = ContextCompat.getColor(context, R.color.blue_primary)
    private var areaColor: Int? = null
    private var yMin = 0f
    private var yMax = 100f
    private var yStep = 10f
    private var normalMin: Float? = null
    private var normalMax: Float? = null

    // ----- 코드(setData)로 넣는 숫자 -----
    private var labels: List<String> = emptyList()
    private var values: List<Float> = emptyList()

    private fun dp(v: Float) = v * resources.displayMetrics.density

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.chart_grid)
        strokeWidth = dp(1f)
    }
    private val axisTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.text_tertiary)
        textSize = dp(10f)
    }
    private val bandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.green_band)
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(2.5f)
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val areaPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pointFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val pointStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1.5f)
    }
    private val pointCenterPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val linePath = Path()
    private val areaPath = Path()

    init {
        // XML에 적은 app:chart... 속성 읽기
        context.withStyledAttributes(attrs, R.styleable.SimpleLineChartView) {
            lineColor = getColor(R.styleable.SimpleLineChartView_chartLineColor, lineColor)
            if (hasValue(R.styleable.SimpleLineChartView_chartAreaColor)) {
                areaColor = getColor(R.styleable.SimpleLineChartView_chartAreaColor, 0)
            }
            yMin = getFloat(R.styleable.SimpleLineChartView_chartYMin, yMin)
            yMax = getFloat(R.styleable.SimpleLineChartView_chartYMax, yMax)
            yStep = getFloat(R.styleable.SimpleLineChartView_chartYStep, yStep)
            if (hasValue(R.styleable.SimpleLineChartView_chartNormalMin) &&
                hasValue(R.styleable.SimpleLineChartView_chartNormalMax)
            ) {
                normalMin = getFloat(R.styleable.SimpleLineChartView_chartNormalMin, 0f)
                normalMax = getFloat(R.styleable.SimpleLineChartView_chartNormalMax, 0f)
            }
        }

        // Design 탭 미리보기 전용 예시 데이터 (실제 앱에서는 실행되지 않음)
        if (isInEditMode) {
            val range = yMax - yMin
            labels = listOf("10시", "11시", "12시", "13시", "14시", "15시", "16시", "17시")
            values = listOf(0.55f, 0.62f, 0.58f, 0.66f, 0.60f, 0.57f, 0.63f, 0.59f)
                .map { yMin + range * it }
        }
    }

    /**
     * 적정 범위 띠(초록 영역) 바꾸기. XML의 app:chartNormalMin/Max 대신 코드에서 정할 때 사용.
     * 예) 작물이 바뀌면 binding.chart.setNormalRange(22f, 28f)
     */
    fun setNormalRange(min: Float, max: Float) {
        normalMin = min
        normalMax = max
        invalidate()
    }

    /** 그래프에 그릴 숫자 넣기. 넣으면 바로 다시 그려짐 */
    fun setData(labels: List<String>, values: List<Float>) {
        this.labels = labels
        this.values = values
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (values.isEmpty()) {
            axisTextPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(context.getString(R.string.chart_no_data), width / 2f, height / 2f, axisTextPaint)
            return
        }

        // 그래프가 그려질 영역 (왼쪽: y축 숫자 자리, 아래: x축 글자 자리, 위: 여백)
        val left = dp(28f)
        val right = width - dp(12f)
        val top = dp(12f)
        val bottom = height - dp(24f)

        fun xAt(i: Int) =
            if (values.size == 1) (left + right) / 2f
            else left + (right - left) * i / (values.size - 1)

        fun yAt(v: Float) =
            bottom - (bottom - top) * ((v - yMin) / (yMax - yMin)).coerceIn(0f, 1f)

        // 1) 적정 범위 띠 (눈금선보다 먼저 깔아서 선이 위에 보이게)
        val nMin = normalMin
        val nMax = normalMax
        if (nMin != null && nMax != null) {
            canvas.drawRect(left, yAt(nMax), right, yAt(nMin), bandPaint)
        }

        // 2) 가로 눈금선 + y축 숫자
        axisTextPaint.textAlign = Paint.Align.RIGHT
        var tick = yMin
        while (tick <= yMax + 0.001f) {
            val y = yAt(tick)
            canvas.drawLine(left, y, right, y, gridPaint)
            canvas.drawText(formatNumber(tick), left - dp(6f), y + dp(3.5f), axisTextPaint)
            tick += yStep
        }

        // 3) 선 경로: 점과 점 사이를 부드러운 곡선으로 연결
        linePath.reset()
        linePath.moveTo(xAt(0), yAt(values[0]))
        for (i in 1..values.lastIndex) {
            val prevX = xAt(i - 1)
            val prevY = yAt(values[i - 1])
            val x = xAt(i)
            val y = yAt(values[i])
            val midX = (prevX + x) / 2f
            linePath.cubicTo(midX, prevY, midX, y, x, y)
        }

        // 4) 선 아래 그라데이션 채우기
        areaColor?.let { color ->
            areaPath.reset()
            areaPath.addPath(linePath)
            areaPath.lineTo(xAt(values.lastIndex), bottom)
            areaPath.lineTo(xAt(0), bottom)
            areaPath.close()
            areaPaint.shader = LinearGradient(
                0f, top, 0f, bottom,
                ColorUtils.setAlphaComponent(color, 70),
                ColorUtils.setAlphaComponent(color, 0),
                Shader.TileMode.CLAMP
            )
            canvas.drawPath(areaPath, areaPaint)
        }

        // 5) 선 그리기
        linePaint.color = lineColor
        canvas.drawPath(linePath, linePaint)

        // 6) 점: 점이 많으면 마지막(= 지금 값)만, 적으면 전부
        pointStrokePaint.color = lineColor
        pointCenterPaint.color = lineColor
        haloPaint.color = ColorUtils.setAlphaComponent(lineColor, 50)
        val drawAllDots = values.size <= MAX_POINTS_WITH_DOTS
        values.forEachIndexed { i, value ->
            val isLast = i == values.lastIndex
            if (!drawAllDots && !isLast) return@forEachIndexed
            val x = xAt(i)
            val y = yAt(value)
            if (isLast) {
                canvas.drawCircle(x, y, dp(9f), haloPaint)          // 옅은 테두리 (지금 값 강조)
                canvas.drawCircle(x, y, dp(5f), pointFillPaint)
                canvas.drawCircle(x, y, dp(5f), pointStrokePaint)
                canvas.drawCircle(x, y, dp(2.5f), pointCenterPaint)
            } else {
                canvas.drawCircle(x, y, dp(3f), pointFillPaint)
                canvas.drawCircle(x, y, dp(3f), pointStrokePaint)
            }
        }

        // 7) x축 시간 글자: 최대 MAX_X_LABELS 개, 같은 글자 연속은 한 번만, 양 끝은 화면 안으로
        axisTextPaint.textAlign = Paint.Align.CENTER
        val count = minOf(labels.size, values.size)
        if (count > 0) {
            val step = maxOf(1, (count + MAX_X_LABELS - 1) / MAX_X_LABELS)
            var lastDrawn: String? = null
            for (i in 0 until count step step) {
                val label = labels[i]
                if (label.isBlank() || label == lastDrawn) continue
                val half = axisTextPaint.measureText(label) / 2f
                val x = xAt(i).coerceIn(left + half, right - half)
                canvas.drawText(label, x, height - dp(6f), axisTextPaint)
                lastDrawn = label
            }
        }
    }

    private fun formatNumber(v: Float): String =
        if (v % 1f == 0f) v.toInt().toString() else "%.1f".format(v)
}
