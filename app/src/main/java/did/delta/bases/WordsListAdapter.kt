package did.delta.bases

import android.content.Context
import android.view.LayoutInflater

import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.TextView
import did.delta.R

open class WordsListAdapter(context: Context?, private var layoutInflater:
            LayoutInflater = LayoutInflater.from(context),
                            private var itemDelete: OnItemDeletedListner?) :
        ArrayAdapter<WordsListRow>(context, R.id.list_item), View.OnClickListener {

    abstract class OnItemDeletedListner {
        abstract fun onItemDelete(position: Int)
    }

    private val deleteWord = View.OnClickListener {
        itemDelete?.onItemDelete((it as ImageButton).tag as Int)
    }

    private var lastIt: TextView? = null

    override fun onClick(it: View) {
        val descr = it as TextView
        if(descr.maxLines > 1) {
            descr.setSingleLine()
            lastIt = null
        }
        else {
            lastIt?.let {
                if(it.maxLines > 1)
                    it.setSingleLine()
            }
            it.maxLines = Int.MAX_VALUE
            lastIt = it
        }
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val lay = convertView ?: layoutInflater.inflate(R.layout.list_words_item, parent, false)
        val item = getItem(position)

        lay.findViewById<TextView>(R.id.item_word).text = item.word
        val descr = lay.findViewById<TextView>(R.id.item_description)
        descr.text = item.description
        descr.setOnClickListener(this)
        lay.findViewById<TextView>(R.id.item_results).text = item.results

        itemDelete?.let {
            val btn = lay.findViewById<ImageButton>(R.id.item_delete)
            btn.visibility = View.VISIBLE
            btn.setOnClickListener(deleteWord)
            btn.tag = position
        }

        return lay
    }

    fun indexOf(word: String) : Int? {
        return (0 until count).firstOrNull { word == getItem(it).word }
    }

    open fun append(word: String, second: String) {
        add(WordsListRow(word, "", second))
    }
}
