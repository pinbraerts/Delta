package did.delta

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.TabLayout
import android.support.transition.TransitionManager
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.MenuItemCompat
import android.support.v4.view.ViewPager
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.SearchView
import android.support.v7.widget.Toolbar
import android.text.InputFilter
import android.text.Spanned
import android.view.*
import android.widget.*

import did.delta.bases.*
import did.delta.util.*

class ListActivity : AppCompatActivity() {

    class WordsPageAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

        companion object {
            val tables = arrayOf(
                    "user_words",
                    "words"
            )

            val gameTypes = arrayOf(
                    "normal"
            )
        }

        override fun getItem(position: Int): Fragment {
            return WordsFragment.newInstance(tables[position % tables.count()],
                    gameTypes[position / tables.count()])
        }

        override fun getCount(): Int {
            return tables.count() * gameTypes.count()
        }

        override fun getPageTitle(position: Int): CharSequence {
            return when (position) {
                0 -> "Пользовательские"
                1 -> "Встроенные"
                else -> super.getPageTitle(position)
            }
        }
    }

    class WordsFragment : Fragment() {
        lateinit var helper: DBHelper
        lateinit var adapter: SSWordsListAdapter
        var wordsTableName: String? = null
        protected val gameType: String by lazy {
            arguments.getString("game_type")
        }
        protected val filter:
                ((CharSequence, Int, Int, Spanned, Int, Int) -> Pair<Boolean, CharSequence?>)?
                by lazy {
            filters[gameType]
        }

        private fun noSuchWord(word: String, description: String) {
            wordsTableName?.let {
                helper.add(it, "word" to word, "description" to description)
            }
            adapter.add(word to description)
        }

        override fun onCreateView(inflater: LayoutInflater?,
                                  container: ViewGroup?, savedInstanceState: Bundle?): View? {
            val coordl = inflater!!.inflate(R.layout.activity_list_fragment, container,
                    false) as CoordinatorLayout
            helper = DBHelper(context)

            if(hasDatabase[gameType] == true)
                wordsTableName = "${arguments.getString("table_type")}_$gameType"
            val list = coordl.findViewById<ListView>(R.id.words_list)
            adapter = SSWordsListAdapter(context,
                if(wordsTableName?.startsWith("user_words") == true)
                    object: WordsListAdapter.OnItemDeletedListner() {
                        override fun onItemDelete(item: Pair<String, String>) {
                            wordsTableName?.let {
                                helper.delete(it, "word", item.first)
                            }
                        }
                    }
                else null,
                if(hasDatabase[gameType] == true) helper.readAll2(wordsTableName!!)
                else arrayListOf()
            )
            if (wordsTableName?.startsWith("user_words") == true) {
                val fab = coordl.findViewById<FloatingActionButton>(R.id.list_fab)
                fab.visibility = View.VISIBLE
                fab.setOnClickListener {
                    val textWord = EditText(context)
                    textWord.hint = "Введите слово"
                    filter?.let {
                        textWord.filters = arrayOf(InputFilter { p1, p2, p3, p4, p5, p6 ->
                            val (en, res) = it(p1, p2, p3, p4, p5, p6)
                            textWord.tag = en
                            res
                        })
                    }
                    val textDescription = EditText(context)
                    textDescription.setHint(R.string.enter_description)
                    val lay = LinearLayout(context)
                    lay.orientation = LinearLayout.VERTICAL
                    lay.addView(textWord)
                    lay.addView(textDescription)
                    lay.setPadding(10, 10, 10, 10)
                    AlertDialog.Builder(context)
                            .setTitle("Новое слово")
                            .setView(lay)
                            .setPositiveButton(android.R.string.ok) { _, _ ->
                                val word = textWord.text.toString()
                                val description = textDescription.text.toString()
                                if (textWord.tag as Boolean) {
                                    if(helper.contains(wordsTableName!!, word))
                                        context.toast(context.getString(R.string.word_is, word))
                                    else
                                        noSuchWord(word, description)
                                } else context.toast(getString(R.string.word_invalid, word))
                            }
                            .setNegativeButton(android.R.string.no, null)
                            .show()
                }

                list.setOnScrollListener(object : AbsListView.OnScrollListener {
                    var last = 0

                    override fun onScrollStateChanged(absListView: AbsListView, i: Int) {}

                    override fun onScroll(absListView: AbsListView,
                                          first: Int, count: Int, total: Int) {
                        val d = first - last
                        if (d > 0 && fab.visibility == View.VISIBLE)
                            fab.hide()
                        else if (d < 0 && fab.visibility != View.VISIBLE) fab.show()
                        last = first
                    }
                })
            } else list.isFastScrollEnabled = true
            list.adapter = adapter

            return coordl
        }

        companion object {

            fun newInstance(tableType: String, gameType: String): WordsFragment {
                val args = Bundle()
                args.putString("table_type", tableType)
                args.putString("game_type", gameType)
                val fragment = WordsFragment()
                fragment.arguments = args
                return fragment
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.app_bar_search -> {
                TransitionManager.beginDelayedTransition(findViewById<View>(R.id.wordsToolbar) as ViewGroup)
                MenuItemCompat.expandActionView(item)
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.words_list_activity_menu, menu)

        val toolbar = findViewById<View>(R.id.wordsToolbar)

        val item = menu.findItem(R.id.app_bar_search)
        MenuItemCompat.setOnActionExpandListener(item,
                object : MenuItemCompat.OnActionExpandListener {
            @SuppressLint("NewApi")
            override fun onMenuItemActionExpand(menuItem: MenuItem): Boolean {
                val anim: ValueAnimator = when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->
                        ValueAnimator.ofArgb(
                                resources.getColor(R.color.colorPrimary, null), Color.WHITE)
                    android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP ->
                        ValueAnimator.ofArgb(resources.getColor(R.color.colorPrimary), Color.WHITE)
                    else -> ValueAnimator.ofObject(ArgbEvaluator(),
                            resources.getColor(R.color.colorPrimary), Color.WHITE)
                }
                anim.duration = 220
                anim.addUpdateListener {
                    valueAnimator -> toolbar.setBackgroundColor(valueAnimator.animatedValue as Int)
                }
                anim.start()
                return true
            }

            @SuppressLint("NewApi")
            override fun onMenuItemActionCollapse(menuItem: MenuItem): Boolean {
                val anim: ValueAnimator = when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->
                        ValueAnimator.ofArgb(Color.WHITE,
                                resources.getColor(R.color.colorPrimary, null))
                    android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP ->
                        ValueAnimator.ofArgb(Color.WHITE, resources.getColor(R.color.colorPrimary))
                    else ->
                        ValueAnimator.ofObject(ArgbEvaluator(), Color.WHITE,
                                resources.getColor(R.color.colorPrimary))
                }

                val vp = findViewById<ViewPager>(R.id.wordsViewPager)
                (0 until vp.childCount).forEach {
                    val list = (vp.getChildAt(it) as CoordinatorLayout)
                            .findViewById<ListView>(R.id.words_list)
                    (list.adapter as SSWordsListAdapter).unColorItems()
                }

                anim.duration = 440
                anim.addUpdateListener {
                    valueAnimator -> toolbar.setBackgroundColor(valueAnimator.animatedValue as Int)
                }
                anim.start()
                return true
            }
        })

        val sv = item.actionView as SearchView

        sv.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return true
            }

            override fun onQueryTextChange(query: String): Boolean {
                var s = false
                val vp = findViewById<ViewPager>(R.id.wordsViewPager)
                (0 until vp.childCount).forEach {
                    val list = (vp.getChildAt(it) as CoordinatorLayout)
                            .findViewById<ListView>(R.id.words_list)
                    val (res, i) = (list.adapter as SSWordsListAdapter).search(query)
                    if(i >= 0) list.setSelection(i)
                    s = s || res
                }

                if(!s) toast("Нет слова${
                    if(query.length != 4) ", начинающегося с \""
                    else " \""}$query\"!")
                return true
            }
        })

        val textWord = sv.findViewById<EditText>(android.support.v7.appcompat.R.id.search_src_text)
        textWord.hint = getString(R.string.enter_word_hint)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)
        setSupportActionBar(findViewById<View>(R.id.wordsToolbar) as Toolbar)

        supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setDisplayShowHomeEnabled(true)
        }

        val vp = findViewById<ViewPager>(R.id.wordsViewPager)
        vp.adapter = WordsPageAdapter(supportFragmentManager)

        val tabLayout = findViewById<TabLayout>(R.id.wordsTabLayout)

        tabLayout.setupWithViewPager(vp)
    }
}

