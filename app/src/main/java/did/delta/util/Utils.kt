package did.delta.util

import android.content.Context
import android.text.Spanned
import android.widget.Toast

fun inRussianAlphabet(c: Char) : Boolean {
    return c in 'а'..'я' || c == 'ё'
}

private fun convertRussian(c: Char) : Char {
    return when {
        c == 'ё' -> 'ж'
        c > 'е' -> c + 1
        else -> c
    }
}

fun cmpRussian(a: Char, b: Char) : Int {
    return convertRussian(a) - convertRussian(b)
}

fun isIdentical(string: String) : Boolean {
    return string.toSet().count() == string.count()
}

fun generateFilter(maxCount: Int, checker: (Char) -> Boolean) :
        (CharSequence, Int, Int, Spanned, Int, Int) -> Pair<Boolean, CharSequence?> {
    return fun(repl: CharSequence, start: Int, end: Int,
               dest: Spanned, dstart: Int, dend: Int): Pair<Boolean, CharSequence?> {
        val dlen = dend - dstart
        val len = dest.length - dlen + end - start
        return if(len <= maxCount && repl.any { checker(it) } &&
                isIdentical(repl.toString() + dest.toString().removeRange(dstart, dend)))
            (len == maxCount) to null
        else (dlen == maxCount) to ""
    }
}

fun Context.toast(message: CharSequence) =
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
