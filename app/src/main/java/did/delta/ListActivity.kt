package did.delta

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.DialogInterface
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
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.AdapterView
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import did.delta.bases.SortedAndStyledWordsList
import did.delta.bases.WordsListAdapter

import did.delta.util.DBHelper

class ListActivity : AppCompatActivity() {

    internal inner class WordsPageAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

        override fun getItem(position: Int): Fragment {
            return WordsFragment.newInstance(position < 1)
        }

        override fun getCount(): Int {
            return 2
        }

        override fun getPageTitle(position: Int): CharSequence {
            when (position) {
                0 -> return "Пользовательские"
                1 -> return "Встроенные"
            }
            return super.getPageTitle(position)
        }
    }

    class WordsFragment : Fragment() {
        internal var helper: DBHelper? = null
        internal var lastChecked: TextView? = null

        private fun noSuchWord(temword: String, description: String) {
            //helper.add("sdgaS", "Sadg" to "sadg");
        }

        override fun onCreateView(inflater: LayoutInflater?,
                                  container: ViewGroup?, savedInstanceState: Bundle?): View? {
            val view = inflater!!.inflate(R.layout.activity_list_fragment, container,
                    false) as CoordinatorLayout

            val user = arguments.getBoolean("user", false)
            val list = view.findViewById<ListView>(R.id.wordsList1)
            list.adapter = SortedAndStyledWordsList(context, object: WordsListAdapter.OnItemDeletedListner() {
                override fun onItemDelete(position: Int) {

                }
            })
            list.onItemClickListener = AdapterView.OnItemClickListener { _, view, _, _ -> (view as LinearLayout).getChildAt(1).callOnClick() }

            if (user) {
                val fab = FloatingActionButton(context)
                fab.setImageResource(R.drawable.ic_add_fab)
                val lp = CoordinatorLayout.LayoutParams(CoordinatorLayout.LayoutParams.WRAP_CONTENT, CoordinatorLayout.LayoutParams.WRAP_CONTENT)
                lp.anchorGravity = Gravity.END or Gravity.BOTTOM
                lp.setMargins(0, 0, 25, 25)
                lp.anchorId = R.id.wordsList1
                fab.layoutParams = lp
                fab.setOnClickListener {
                    val textWord = EditText(context)
                    textWord.hint = "Введите слово"
                    textWord.filters = arrayOf() // TODO: add filter
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
                            .setPositiveButton(android.R.string.ok) { dialogInterface, i ->
                                val word = textWord.text.toString()
                                val description = textDescription.text.toString()
                                if (word.length == 4) {
                                    /*try {
                                                Cursor c = helper.getReadableDatabase()
                                                        //.rawQuery(getString(R.string.get_count_cond, DBHelper.WORDS_TABLE_NAME), new String[]{word});
                                                if (c.moveToFirst() && c.getInt(0) > 0) {
                                                    Toast.makeText(getContext(), "Слово \"" + word + "\" уже есть!", Toast.LENGTH_SHORT).show();
                                                    c.close();
                                                    helper.close();
                                                    return;
                                                }
                                                //((WordAdapter)list.getAdapter()).addElem(word, description);
                                                c.close();
                                            } finally {
                                                helper.close();
                                            }*/
                                    noSuchWord(word, description)
                                } else
                                    Toast.makeText(context, "Слово слишком короткое",
                                            Toast.LENGTH_SHORT).show()
                            }
                            .setNegativeButton(android.R.string.no, null)
                            .show()
                }
                view.addView(fab)

                list.setOnScrollListener(object : AbsListView.OnScrollListener {
                    internal var last = 0

                    override fun onScrollStateChanged(absListView: AbsListView, i: Int) {}

                    override fun onScroll(absListView: AbsListView, first: Int, count: Int, total: Int) {
                        val d = first - last
                        if (d > 0 && fab.visibility == View.VISIBLE)
                            fab.hide()
                        else if (d < 0 && fab.visibility != View.VISIBLE) fab.show()
                        last = first
                    }
                })
            } else
                list.isFastScrollEnabled = true

            return view
        }

        companion object {

            fun newInstance(user: Boolean): WordsFragment {
                val args = Bundle()
                args.putBoolean("user", user)
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
        MenuItemCompat.setOnActionExpandListener(item, object : MenuItemCompat.OnActionExpandListener {
            override fun onMenuItemActionExpand(menuItem: MenuItem): Boolean {
                val anim: ValueAnimator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    anim = ValueAnimator.ofArgb(resources.getColor(R.color.colorPrimary, null), Color.WHITE)
                else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP)
                    anim = ValueAnimator.ofArgb(resources.getColor(R.color.colorPrimary), Color.WHITE)
                else
                    anim = ValueAnimator.ofObject(ArgbEvaluator(), resources.getColor(R.color.colorPrimary), Color.WHITE)
                anim.duration = 220
                anim.addUpdateListener { valueAnimator -> toolbar.setBackgroundColor(valueAnimator.animatedValue as Int) }
                anim.start()
                return true
            }

            override fun onMenuItemActionCollapse(menuItem: MenuItem): Boolean {
                val anim: ValueAnimator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    anim = ValueAnimator.ofArgb(Color.WHITE, resources.getColor(R.color.colorPrimary, null))
                else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP)
                    anim = ValueAnimator.ofArgb(Color.WHITE, resources.getColor(R.color.colorPrimary))
                else
                    anim = ValueAnimator.ofObject(ArgbEvaluator(), Color.WHITE, resources.getColor(R.color.colorPrimary))

                val vp = findViewById<ViewPager>(R.id.wordsViewPager)
                var list = (vp.getChildAt(0) as CoordinatorLayout).getChildAt(0) as ListView
                //((WordsFragment.WordAdapter)list.getAdapter()).unColorItems();
                list = (vp.getChildAt(1) as CoordinatorLayout).getChildAt(0) as ListView
                //((WordsFragment.WordAdapter)list.getAdapter()).unColorItems();

                anim.duration = 440
                anim.addUpdateListener { valueAnimator -> toolbar.setBackgroundColor(valueAnimator.animatedValue as Int) }
                anim.start()
                return true
            }
        })

        val sv = item.actionView as SearchView

        sv.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return true
            }

            private fun search(query: String) {
                val vp = findViewById<ViewPager>(R.id.wordsViewPager)
                var list = (vp.getChildAt(0) as CoordinatorLayout).getChildAt(0) as ListView
                //                boolean s = ((WordsFragment.WordAdapter)list.getAdapter()).search(query, list);
                list = (vp.getChildAt(1) as CoordinatorLayout).getChildAt(0) as ListView
                //              s = s || ((WordsFragment.WordAdapter)list.getAdapter()).search(query, list);
                //  if(!s) Toast.makeText(ListActivity.this, "Нет слова" + (query.length() == 4 ? ", начинающегося с \"" : " \"") + query + "\"!", Toast.LENGTH_SHORT).show();
            }

            override fun onQueryTextChange(newText: String): Boolean {
                search(newText)
                return true
            }
        })

        val textWord = sv.findViewById<EditText>(android.support.v7.appcompat.R.id.search_src_text)
        textWord.hint = "Введите слово"
        textWord.filters = arrayOf(InputFilter { charSequence, i, i1, spanned, i2, i3 ->
            val (_, res) =
        })

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

