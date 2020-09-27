package host.stjin.anonaddy.utils

import java.math.RoundingMode

object NumberUtils {

    fun roundOffDecimal(number: Double): Double? {
        return number.toBigDecimal().setScale(1, RoundingMode.FLOOR).toDouble()
    }
}