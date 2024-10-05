package com.kusamaru.standroid.tool

/**
 * "application/x-www-form-urlencoded"形式にエンコード
 */
fun encodeToForm(s: String): String {
    var buf = ""
    for (c in s) {
        buf += when (c) {
            ':' -> "%3A"
            '/' -> "%2F"
            '?' -> "%3F"
            '#' -> "%23"
            '[' -> "%5B"
            ']' -> "%5D"
            '@' -> "%40"
            '!' -> "%21"
            '$' -> "%24"
            '&' -> "%26"
            '\'' -> "%27"
            '(' -> "%28"
            ')' -> "%29"
            '*' -> "%2A"
            '+' -> "%2B"
            ',' -> "%2C"
            ';' -> "%3B"
            '=' -> "%3D"
            '%' -> "%25"
            ' ' -> "%20"
            else -> c
        }
    }
    return buf
}