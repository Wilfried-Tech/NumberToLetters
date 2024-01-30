@file:JvmName("NumberToLetters")

package com.wilfried.tech.toletters.tools

fun main(args: Array<String>) {
    for (i in 1 until args.size) {
        try {
            println(toLetters(args[i]))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}


private val numberSuffix = arrayOf(
    "",
    "mille",
    "million",
    "milliard",
    "billion",
    "billiard",
    "trillion",
    "trilliard",
    "quadrillion",
    "quadrilliard"
)

val MAX_VALUE_LENGTH = numberSuffix.size * 3

private val unitLetters = arrayOf(
    "",
    "un",
    "deux",
    "trois",
    "quatre",
    "cinq",
    "six",
    "sept",
    "huit",
    "neuf",
    "dix",
    "onze",
    "douze",
    "treize",
    "quatorze",
    "quinze",
    "seize",
    "dix-sept",
    "dix-huit",
    "dix-neuf"
)

private val tensLetters = arrayOf(
    "",
    "dix",
    "vingt",
    "trente",
    "quarante",
    "cinquante",
    "soixante",
    "soixante",
    "quatre-vingt",
    "quatre-vingt"
)

var separator = "-"

private fun toLettersFrom0To999(number: Int): String {
    var unitOut: String
    var tensOut: String
    val hundredsOut: String
    var output = ""
    val unit = number % 10
    var tens = number % 100 - unit
    var hundreds = number % 1000 - (tens + unit)
    tens /= 10
    hundreds /= 100
    if (number == 0) {
        output = "zero"
    } else {
        unitOut = if (unit == 1 && tens > 0) "et-" else ""
        unitOut += unitLetters[unit]
        if (tens == 1 && unit > 0) {
            tensOut = unitLetters[unit + 10]
            unitOut = ""
        } else if (tens == 7 || tens == 9) {
            tensOut =
                tensLetters[tens] + "-" + (if (tens == 7 && unit == 1) "et-" else "") + unitLetters[10 + unit]
            unitOut = ""
        } else {
            tensOut = tensLetters[tens]
        }
        tensOut += if (unit == 0 && tens == 8) "s" else ""
        hundredsOut =
            (if (hundreds > 1) unitLetters[hundreds] + "-" else "") + (if (hundreds > 0) "cent" else "") + if (hundreds > 1 && tens == 0) "s" else ""
        output += hundredsOut
        output += (if (output.isNotEmpty() && !output.endsWith("-") && tensOut.isNotEmpty()) "-" else "") + tensOut
        output += (if (output.isNotEmpty() && !output.endsWith("-") && unitOut.isNotEmpty()) "-" else "") + unitOut
    }
    return output
}

private fun split3(strNumber: String): ArrayList<Int> {
    val list = ArrayList<Int>()
    val temp = StringBuilder(strNumber).reverse().toString().toCharArray()
    val numberParts = ArrayList<String>()
    var builder = StringBuilder()
    var i = 0
    var count = 0
    while (i < temp.size) {
        if (count == 3) {
            numberParts.add(builder.reverse().toString())
            builder = StringBuilder()
            count = 0
        }
        builder.append(temp[i])
        i++
        count++
    }
    numberParts.add(builder.reverse().toString())
    for (s in numberParts) {
        list.add(s.toInt())
    }
    list.reverse()
    return list
}

fun toLetters(number: Long): String {
    return toLetters(number.toString())
}

@Throws(IllegalArgumentException::class)
fun toLetters(number: String): String {
    var strNumber = number
    strNumber = strNumber.trim { it <= ' ' }

    if (!strNumber.matches("-?\\d+".toRegex())) {
        throw NumberFormatException("la valeur entrez n' est pas un nombre entier")
    }

    require(strNumber.length <= MAX_VALUE_LENGTH) { "la valeur entrée est plus grand que la constante MAX_VALUE_LENGTH" }

    val negative = strNumber.startsWith("-")
    if (negative) {
        strNumber = strNumber.substring(1)
    }
    val output = StringBuilder()
    val parts: ArrayList<Int> = split3(strNumber)
    parts.reverse()
    for (i in parts.indices.reversed()) {
        val value = toLettersFrom0To999(parts[i])
        if (value == "zero" && numberSuffix[i].isEmpty() && parts.size == 1) {
            output.append(value)
        } else if (value != "zero") {
            if (value == "un" && numberSuffix[i] == "mille") {
                output.append(" mille")
            } else {
                output.append(" ").append(value).append(" ").append(numberSuffix[i])
            }
        }
    }
    if (negative) output.insert(0, "moins")
    return output.toString().trim { it <= ' ' }.replace("-".toRegex(), separator)
}

