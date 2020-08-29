package host.stjin.anonaddy.utils

import java.math.RoundingMode
import java.text.DecimalFormat

object NumberUtils {

    fun roundOffDecimal(number: Double): Double? {
        val df = DecimalFormat("#.#")
        df.roundingMode = RoundingMode.FLOOR
        return df.format(number).toDouble()
    }
}