package did.delta.bases

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import did.delta.R
import did.delta.util.cmpRussian


class SortedAndStyledWordsList(context: Context,
                               onItemDeletedListener: OnItemDeletedListner? = null,
                               layoutInflater: LayoutInflater = LayoutInflater.from(context)) :
        WordsListAdapter(context, layoutInflater, onItemDeletedListener) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val lay = super.getView(position, convertView, parent)
        lay.findViewById<TextView>(R.id.item_word).setText((
                if(position in indexColoredStart until indexColoredEnd)
                    colored!![position - indexColoredStart] as CharSequence
                else getItem(position).word), TextView.BufferType.SPANNABLE)
        return lay
    }

    private fun cmpStrings(first: String, second: String): Int {
        var i = 0
        while (i < first.length && i < second.length && second[i] == first[i]) ++i

        val c = if (first.length > i)
            if (second.length > i) cmpRussian(first[i], second[i])
            else 1 else if (second.length > i) -1 else 0
        return if (c > 0) 1 else if (c == 0) 0 else -1
    }

    var indexColoredStart = 0
    var indexColoredEnd = 0
    var colored: Array<Spannable?>? = null

    private fun unColorItems() {
        colored = null
        indexColoredEnd = 0
        indexColoredStart = 0
        notifyDataSetChanged()
    }

    private fun colorItems(indexStart: Int, indexEnd: Int, length: Int) {
        indexColoredStart = indexStart
        indexColoredEnd = indexEnd
        val size = indexColoredEnd - indexColoredStart
        colored = arrayOfNulls(size)
        for (i in 0 until size) {
            val bui = SpannableString(getItem(indexColoredStart + i).word)
            bui.setSpan(ForegroundColorSpan(Color.YELLOW), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                bui.setSpan(BackgroundColorSpan(
                                context.resources.getColor(R.color.colorAccent, null)),
                        0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            else
                bui.setSpan(BackgroundColorSpan(context.resources.getColor(R.color.colorAccent)),
                        0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            colored!![i] = bui
        }
        notifyDataSetChanged()
    }

    override fun append(word: String, second: String) {
        add(WordsListRow(word, second))
    }

    override fun add(item: WordsListRow) {
        var a = 0
        var b = count
        var i: Int
        i = (a + b) / 2
        while (a < b) {
            when (cmpStrings(item.word, getItem(i).word)) {
                -1 -> a = i + 1
                1 -> b = i
            }
            i = (a + b) / 2
        }
        super.insert(item, a)
    }
}
