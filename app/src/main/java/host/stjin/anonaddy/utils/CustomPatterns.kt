@file:Suppress("RegExpSimplifiable")

package host.stjin.anonaddy.utils

import java.util.regex.Pattern

object CustomPatterns {
    // I am too kind for humanity....
    // https://gitlab.com/Stjin/anonaddy-android/-/issues/31
    val EMAIL_ADDRESS: Pattern = Pattern.compile(
        "[a-zA-Z0-9~\\/\\+\\.\\_\\%\\-\\+]{1,256}" +
                "\\@" +
                "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                "(" +
                "\\." +
                "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
                ")+"
    )
}