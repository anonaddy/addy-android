package host.stjin.anonaddy.utils

import java.math.RoundingMode

object NumberUtils {

    fun roundOffDecimal(number: Double): Float {
        return number.toBigDecimal().setScale(1, RoundingMode.FLOOR).toFloat()
    }
}