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
 *     app:chartNormalMin  app:chartNormalMax  app:chartMaxLabelUnit
 * ▸ 숫자는 코드에서 넣기
 *     binding.chart.setData(labels, values)
 *
 * Design 탭 미리보기에서는 예시 숫자로 그려지므로, XML만 봐도 완성 모양을 확인할 수 있습니다.
 * 이 파일은 "그리는 방법"만 담당해서, 화면 작업할 때는 거의 건드릴 필요가 없습니다.
 */
class SimpleLineChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // ----- XML 속성으로 정하는 모양 -----
    private var lineColor = ContextCompat.getColor(context, R.color.blue_primary)
    private var areaColor: Int? = null
    private var yMin = 0f
    private var yMax = 100f
    private var yStep = 10f
    private var normalMin: Float? = null
    private var normalMax: Float? = null
    private var maxLabelUnit: String? = null

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
        strokeWidth = dp(2f)
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
    private val maxLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = dp(10f)
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }
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
            maxLabelUnit = getString(R.styleable.SimpleLineChartView_chartMaxLabelUnit)
        }

        // Design 탭 미리보기 전용 예시 데이터 (실제 앱에서는 실행되지 않음)
        if (isInEditMode) {
            val range = yMax - yMin
            labels = listOf("10시", "11시", "12시", "13시", "14시", "15시", "16시", "17시")
            values = listOf(0.70f, 0.74f, 0.69f, 0.72f, 0.68f, 0.70f, 0.73f, 0.67f)
                .map { yMin + range * it }
        }
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

        // 그래프가 그려질 영역 (왼쪽: y축 숫자 자리, 아래: x축 글자 자리, 위: 최고점 글자 자리)
        val left = dp(28f)
        val right = width - dp(10f)
        val top = dp(16f)
        val bottom = height - dp(22f)

        fun xAt(i: Int) =
            if (values.size == 1) (left + right) / 2f
            else left + (right - left) * i / (values.size - 1)

        fun yAt(v: Float) =
            bottom - (bottom - top) * ((v - yMin) / (yMax - yMin)).coerceIn(0f, 1f)

        // 1) 가로 눈금선 + y축 숫자
        axisTextPaint.textAlign = Paint.Align.RIGHT
        var tick = yMin
        while (tick <= yMax + 0.001f) {
            val y = yAt(tick)
            canvas.drawLine(left, y, right, y, gridPaint)
            canvas.drawText(formatNumber(tick), left - dp(6f), y + dp(3.5f), axisTextPaint)
            tick += yStep
        }

        // 2) 적정 범위 띠
        val nMin = normalMin
        val nMax = normalMax
        if (nMin != null && nMax != null) {
            canvas.drawRect(left, yAt(nMax), right, yAt(nMin), bandPaint)
        }

        // 3) 꺾은선 경로 만들기
        linePath.reset()
        values.forEachIndexed { i, value ->
            if (i == 0) linePath.moveTo(xAt(i), yAt(value)) else linePath.lineTo(xAt(i), yAt(value))
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
                ColorUtils.setAlphaComponent(color, 90),
                ColorUtils.setAlphaComponent(color, 0),
                Shader.TileMode.CLAMP
            )
            canvas.drawPath(areaPath, areaPaint)
        }

        // 5) 선 그리기
        linePaint.color = lineColor
        canvas.drawPath(linePath, linePaint)

        // 6) 점 (마지막 점 = 현재 값은 크게 강조)
        pointStrokePaint.color = lineColor
        pointCenterPaint.color = lineColor
        values.forEachIndexed { i, value ->
            val x = xAt(i)
            val y = yAt(value)
            val radius = if (i == values.lastIndex) dp(5f) else dp(3f)
            canvas.drawCircle(x, y, radius, pointFillPaint)
            canvas.drawCircle(x, y, radius, pointStrokePaint)
            if (i == values.lastIndex) canvas.drawCircle(x, y, dp(2.5f), pointCenterPaint)
        }

        // 7) 가장 높은 점 위에 값 표시 (예: 26°C)
        maxLabelUnit?.let { unit ->
            val maxIndex = values.indices.maxByOrNull { values[it] } ?: return@let
            maxLabelPaint.color = lineColor
            canvas.drawText(
                formatNumber(values[maxIndex]) + unit,
                xAt(maxIndex), yAt(values[maxIndex]) - dp(8f), maxLabelPaint
            )
        }

        // 8) x축 글자
        axisTextPaint.textAlign = Paint.Align.CENTER
        labels.forEachIndexed { i, label ->
            if (i < values.size) canvas.drawText(label, xAt(i), height - dp(6f), axisTextPaint)
        }
    }

    private fun formatNumber(v: Float): String =
        if (v % 1f == 0f) v.toInt().toString() else "%.1f".format(v)
}
