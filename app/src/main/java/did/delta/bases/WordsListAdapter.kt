package did.delta.bases

import android.content.Context
import android.view.LayoutInflater

import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.TextView
import did.delta.R

open class WordsListAdapter(context: Context?,
                            private var itemDelete: OnItemDeletedListner? = null,
                            arrayList: ArrayList<Pair<String, String>> = arrayListOf(),
                            private var layoutInflater: LayoutInflater =
                                LayoutInflater.from(context)) :
        ArrayAdapter<Pair<String, String>>(context, R.id.list_item, arrayList),
        View.OnClickListener {

    abstract class OnItemDeletedListner {
        abstract fun onItemDelete(item: Pair<String, String>)
    }

    private val deleteWord = View.OnClickListener {
        val pos = (it as ImageButton).tag as Int
        val item = getItem(pos)
        itemDelete?.onItemDelete(item)
        remove(item)
        notifyDataSetChanged()
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
            it.setSingleLine(false)
            lastIt = it
        }
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val lay = convertView ?: layoutInflater.inflate(R.layout.list_words_item, parent, false)
        val item = getItem(position)

        lay.findViewById<TextView>(R.id.item_word).text = item.first
        val res = lay.findViewById<TextView>(R.id.item_results)
        res.visibility = View.VISIBLE
        res.text = item.second

        return lay
    }

    fun indexOf(word: String) : Int? {
        return (0 until count).firstOrNull { word == getItem(it).first }
    }
}
