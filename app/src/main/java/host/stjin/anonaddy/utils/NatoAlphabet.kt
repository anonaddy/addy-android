package host.stjin.anonaddy.utils

object NatoAlphabet {

    data class NatoItem(val character: Char, val word: String)

    private val natoMap = hashMapOf(
        'A' to "Alpha", 'B' to "Bravo", 'C' to "Charlie", 'D' to "Delta", 'E' to "Echo",
        'F' to "Foxtrot", 'G' to "Golf", 'H' to "Hotel", 'I' to "India", 'J' to "Juliett",
        'K' to "Kilo", 'L' to "Lima", 'M' to "Mike", 'N' to "November", 'O' to "Oscar",
        'P' to "Papa", 'Q' to "Quebec", 'R' to "Romeo", 'S' to "Sierra", 'T' to "Tango",
        'U' to "Uniform", 'V' to "Victor", 'W' to "Whiskey", 'X' to "X-ray", 'Y' to "Yankee",
        'Z' to "Zulu",
        '0' to "Zero", '1' to "One", '2' to "Two", '3' to "Three", '4' to "Four",
        '5' to "Five", '6' to "Six", '7' to "Seven", '8' to "Eight", '9' to "Nine",
        '.' to "Period", '@' to "At sign"
    )

    fun getWord(char: Char): NatoItem {
        val upperChar = char.uppercaseChar()
        val word = natoMap[upperChar] ?: upperChar.toString()
        return NatoItem(upperChar, word)
    }
}
