package did.delta;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.transition.TransitionManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.InputFilter;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class ListActivity extends AppCompatActivity {

    class WordsPageAdapter extends FragmentPagerAdapter {

        public WordsPageAdapter(FragmentManager fm) {
             super(fm);
         }

        @Override
        public Fragment getItem(int position) {
            return WordsFragment.newInstance(position < 1);
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch(position) {
                case 0: return "Пользовательские";
                case 1: return "Встроенные";
            }
            return super.getPageTitle(position);
        }
    }

    public static class WordsFragment extends Fragment implements View.OnClickListener {
        DBHelper helper;
        TextView lastChecked = null;

        private void noSuchWord (String temword, String description) {
            try {
                SQLiteDatabase db = helper.getWritableDatabase();
                ContentValues cv = new ContentValues();
                cv.put("word", temword);
                cv.put("description", description);
                cv.put("accepted", false);
                db.insertOrThrow(DBHelper.WORDS_TABLE_NAME, null, cv);
            } finally {
                helper.close();
            }
        }

        @Override
        public void onClick(View view) {
            TextView text = (TextView)view;
            if(lastChecked != null && lastChecked != text) lastChecked.setSingleLine(true);
            text.setSingleLine(text.getMaxLines() > 1);
            lastChecked = text;
        }

        public class WordAdapter extends ArrayAdapter<String> implements View.OnClickListener {

            public ArrayList<String> descriptionsArray = new ArrayList<>();
            boolean user;
            LayoutInflater inf;

            public WordAdapter(Context context, int id, ArrayList<String> arr, boolean usr) {
                super(context, id, arr);
                setNotifyOnChange(true);
                user = usr;
                inf = getLayoutInflater(null);
                helper = new DBHelper(context);

                Cursor c = helper.getReadableDatabase().rawQuery(getString(R.string.cmd_usr_query,
                        DBHelper.WORDS_TABLE_NAME, user ? 0 : 1), null);
                if(c.moveToFirst()) {
                    do addElem(c.getString(c.getColumnIndex("word")),
                            c.getString(c.getColumnIndex("description"))); while(c.moveToNext());
                }
                c.close();
                helper.close();
            }

            public boolean search(String query, ListView list) {
                if(query.length() == 0) {
                    unColorItems();
                    return true;
                }
                int a = 0, b = getCount();
                for(int i = (a + b) / 2; a < b; i = (a + b) / 2) {
                    switch(cmpStrings(getItem(i), query)) {
                        case -1:
                            b = i;
                            break;
                        case 1:
                            if(getItem(i).startsWith(query)) {
                                int start, end;
                                for(start = i - 1; start >= 0 && getItem(start).startsWith(query); --start);
                                ++start;
                                for(end = i + 1; end < getCount() && getItem(end).startsWith(query); ++end);
                                colorItems(start, end, query.length());
                                list.setSelection(start);
                                return true;
                            }
                            a = i + 1;
                            break;
                        case 0:
                            int start, end;
                            for(start = i - 1; start >= 0 && getItem(start).startsWith(query); --start);
                            ++start;
                            for(end = i + 1; end < getCount() && getItem(end).startsWith(query); ++end);
                            colorItems(start, end, query.length());
                            list.setSelection(start);
                            return true;
                    }
                }
                unColorItems();
                return false;
            }

            private char chToNorm(char c) {
                return c == 'ё' ? 'ж' : c > 'е' ? (char)(c + 1) : c;
            }

            private int cmpStrings(String first, String second) {
                int i;
                for(i = 0; i < first.length() && i < second.length() && second.charAt(i) == first.charAt(i); ++i);

                int c = first.length() > i ? second.length() > i ? chToNorm(second.charAt(i)) - chToNorm(first.charAt(i)) : 1 : second.length() > i ? -1 : 0;
                return c > 0 ? 1 : c == 0 ? 0 : -1;
            }

            int indexColoredStart = 0;
            int indexColoredEnd = 0;
            Spannable[] colored;

            private void unColorItems() {
                colored = null;
                indexColoredEnd = 0;
                indexColoredStart = 0;
                notifyDataSetChanged();
            }

            private void colorItems(int indexStart, int indexEnd, int length) {
                indexColoredStart = indexStart;
                indexColoredEnd = indexEnd;
                int size = indexColoredEnd - indexColoredStart;
                colored = new Spannable[size];
                for(int i = 0; i < size; ++i) {
                    SpannableString bui = new SpannableString(getItem(indexColoredStart + i));
                    bui.setSpan(new ForegroundColorSpan(Color.YELLOW), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) bui.setSpan(new BackgroundColorSpan(getResources().getColor(R.color.colorAccent, null)), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    else bui.setSpan(new BackgroundColorSpan(getResources().getColor(R.color.colorAccent)), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    colored[i] = bui;
                }
                notifyDataSetChanged();
            }

            public void addElem(String string, String description) {
                int a = 0, b = getCount(), i;
                for (i = (a + b) / 2; a < b; i = (a + b) / 2) {
                    switch (cmpStrings(string, getItem(i))) {
                        case -1:
                            a = i + 1;
                            break;
                        case 1:
                            b = i;
                            break;
                    }
                }
                descriptionsArray.add(a, description);
                super.insert(string, a);
            }

            @NonNull
            @Override
            public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
                LinearLayout elem = (LinearLayout)(convertView != null ? convertView : inf.inflate(R.layout.list_words_elem, parent, false));
                TextView text = (TextView)elem.findViewById(R.id.descriptionText);
                text.setText(descriptionsArray.get(position));
                text.setOnClickListener(WordsFragment.this);

                text = (TextView)elem.findViewById(R.id.wordText);
                text.setText(position >= indexColoredStart && position < indexColoredEnd ? colored[position - indexColoredStart] : getItem(position), TextView.BufferType.SPANNABLE);

                if (user) {
                    if(elem.getChildCount() < 3) {
                        ImageButton button = new ImageButton(getContext());
                        button.setImageResource(R.drawable.ic_delete_word_button);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) button.setBackgroundColor(getResources().getColor(android.R.color.transparent, null));
                        else button.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                        button.setTag(position);

                        LinearLayout.LayoutParams lay = new LinearLayout.LayoutParams(80, 80);
                        lay.gravity = Gravity.CENTER_VERTICAL;
                        button.setLayoutParams(lay);

                        button.setOnClickListener(this);
                        elem.addView(button);
                    } else elem.getChildAt(2).setTag(position);
                }

                return elem;
            }

            public void removeElem(int position) {
                descriptionsArray.remove(position);
                super.remove(getItem(position));
            }

            @Override
            public void onClick(View view) {
                int pos = (Integer)view.getTag();
                try {
                    helper.getWritableDatabase().
                            delete(DBHelper.WORDS_TABLE_NAME, "word = \"" + super.getItem(pos) + '"', null);
                } finally {
                    helper.close();
                }
                removeElem(pos);
            }
        }

        public WordsFragment() {}

        public static WordsFragment newInstance(boolean user) {
            Bundle args = new Bundle();
            args.putBoolean("user", user);
            WordsFragment fragment = new WordsFragment();
            fragment.setArguments(args);
            return fragment;
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            CoordinatorLayout view = (CoordinatorLayout)inflater.inflate(R.layout.activity_list_fragment, container, false);

            boolean user = getArguments().getBoolean("user", false);
            final ListView list = (ListView)view.findViewById(R.id.wordsList);
            list.setAdapter(new WordAdapter(getContext(),
                    android.R.layout.simple_expandable_list_item_1, new ArrayList<String>(),
                    user));
            list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    ((LinearLayout)view).getChildAt(1).callOnClick();
                }
            });

            if(user) {
                final FloatingActionButton fab = new FloatingActionButton(getContext());
                fab.setImageResource(R.drawable.ic_add_fab);
                CoordinatorLayout.LayoutParams lp = new CoordinatorLayout.LayoutParams(CoordinatorLayout.LayoutParams.WRAP_CONTENT, CoordinatorLayout.LayoutParams.WRAP_CONTENT);
                lp.anchorGravity = Gravity.END | Gravity.BOTTOM;
                lp.setMargins(0, 0, 25, 25);
                lp.setAnchorId(R.id.wordsList);
                fab.setLayoutParams(lp);
                fab.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        final EditText textWord = new EditText(getContext());
                        textWord.setHint("Введите слово");
                        textWord.setFilters(new InputFilter[]{new InputFilter() {
                            @Override
                            public CharSequence filter(CharSequence charSequence, int i, int i1, Spanned spanned, int i2, int i3) {
                                int len = i1 - i + textWord.length();
                                if(charSequence.toString().matches("[а-я,ё]")
                                        && len <= 4
                                        && (charSequence.length() == 0
                                        || textWord.getText()
                                        .toString()
                                        .indexOf(charSequence.charAt(charSequence.length() - 1)) == -1)
                                        ) {
                                    return null;
                                }
                                return "";
                            }
                        }});
                        final EditText textDescription = new EditText(getContext());
                        textDescription.setHint(R.string.enter_description);
                        LinearLayout lay = new LinearLayout(getContext());
                        lay.setOrientation(LinearLayout.VERTICAL);
                        lay.addView(textWord);
                        lay.addView(textDescription);
                        lay.setPadding(10, 10, 10, 10);
                        new AlertDialog.Builder(getContext())
                                .setTitle("Новое слово")
                                .setView(lay)
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        String word = textWord.getText().toString(),
                                            description = textDescription.getText().toString();
                                        if(word.length() == 4) {
                                            try {
                                                Cursor c = helper.getReadableDatabase()
                                                        .rawQuery(getString(R.string.get_count_cond, DBHelper.WORDS_TABLE_NAME), new String[]{word});
                                                if (c.moveToFirst() && c.getInt(0) > 0) {
                                                    Toast.makeText(getContext(), "Слово \"" + word + "\" уже есть!", Toast.LENGTH_SHORT).show();
                                                    c.close();
                                                    helper.close();
                                                    return;
                                                }
                                                ((WordAdapter)list.getAdapter()).addElem(word, description);
                                                c.close();
                                            } finally {
                                                helper.close();
                                            }
                                            noSuchWord(word, description);
                                        }
                                        else
                                            Toast.makeText(getContext(), "Слово слишком короткое",
                                                    Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .setNegativeButton(android.R.string.no, null)
                                .show();
                    }
                });
                view.addView(fab);

                list.setOnScrollListener(new AbsListView.OnScrollListener() {
                    int last = 0;

                    @Override
                    public void onScrollStateChanged(AbsListView absListView, int i) {}

                    @Override
                    public void onScroll(AbsListView absListView, int first, int count, int total) {
                        int d = first - last;
                        if(d > 0 && fab.getVisibility() == View.VISIBLE) fab.hide();
                        else if(d < 0 && fab.getVisibility() != View.VISIBLE) fab.show();
                        last = first;
                    }
                });
            }
            else list.setFastScrollEnabled(true);

            return view;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.app_bar_search:
                TransitionManager.beginDelayedTransition((ViewGroup)findViewById(R.id.wordsToolbar));
                MenuItemCompat.expandActionView(item);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.words_list_activity_menu, menu);

        final View toolbar = findViewById(R.id.wordsToolbar);

        MenuItem item = menu.findItem(R.id.app_bar_search);
        MenuItemCompat.setOnActionExpandListener(item, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem menuItem) {
                ValueAnimator anim;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) anim = ValueAnimator.ofArgb(getResources().getColor(R.color.colorPrimary, null), Color.WHITE);
                else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) anim = ValueAnimator.ofArgb(getResources().getColor(R.color.colorPrimary), Color.WHITE);
                else anim = ValueAnimator.ofObject(new ArgbEvaluator(), getResources().getColor(R.color.colorPrimary), Color.WHITE);
                anim.setDuration(220);
                anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        toolbar.setBackgroundColor((int)valueAnimator.getAnimatedValue());
                    }
                });
                anim.start();
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem menuItem) {
                ValueAnimator anim;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) anim = ValueAnimator.ofArgb(Color.WHITE, getResources().getColor(R.color.colorPrimary, null));
                else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) anim = ValueAnimator.ofArgb(Color.WHITE, getResources().getColor(R.color.colorPrimary));
                else anim = ValueAnimator.ofObject(new ArgbEvaluator(), Color.WHITE, getResources().getColor(R.color.colorPrimary));

                ViewPager vp = (ViewPager)findViewById(R.id.wordsViewPager);
                ListView list = (ListView)((CoordinatorLayout)vp.getChildAt(0)).getChildAt(0);
                ((WordsFragment.WordAdapter)list.getAdapter()).unColorItems();
                list = (ListView)((CoordinatorLayout)vp.getChildAt(1)).getChildAt(0);
                ((WordsFragment.WordAdapter)list.getAdapter()).unColorItems();

                anim.setDuration(440);
                anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        toolbar.setBackgroundColor((int)valueAnimator.getAnimatedValue());
                    }
                });
                anim.start();
                return true;
            }
        });

        SearchView sv = (SearchView)item.getActionView();

        sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return true;
            }

            private void search(String query) {
                ViewPager vp = (ViewPager)findViewById(R.id.wordsViewPager);
                ListView list = (ListView)((CoordinatorLayout)vp.getChildAt(0)).getChildAt(0);
                boolean s = ((WordsFragment.WordAdapter)list.getAdapter()).search(query, list);
                list = (ListView)((CoordinatorLayout)vp.getChildAt(1)).getChildAt(0);
                s = s || ((WordsFragment.WordAdapter)list.getAdapter()).search(query, list);
                if(!s) Toast.makeText(ListActivity.this, "Нет слова" + (query.length() == 4 ? ", начинающегося с \"" : " \"") + query + "\"!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                search(newText);
                return true;
            }
        });

        final EditText textWord = (EditText)sv.findViewById(android.support.v7.appcompat.R.id.search_src_text);
        textWord.setHint("Введите слово");
        textWord.setFilters(new InputFilter[]{new InputFilter() {
            @Override
            public CharSequence filter(CharSequence charSequence, int i, int i1, Spanned spanned, int i2, int i3) {
                int len = i1 - i + textWord.length();
                if(charSequence.toString().matches("[а-я,ё]")
                        && len <= 4
                        && (charSequence.length() == 0
                        || textWord.getText()
                        .toString()
                        .indexOf(charSequence.charAt(charSequence.length() - 1)) == -1)
                        ) {
                    return null;
                }
                return "";
            }
        }});

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        setSupportActionBar((Toolbar)findViewById(R.id.wordsToolbar));

        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        ViewPager vp = (ViewPager)findViewById(R.id.wordsViewPager);
        vp.setAdapter(new WordsPageAdapter(getSupportFragmentManager()));

        TabLayout tabLayout = (TabLayout)findViewById(R.id.wordsTabLayout);

        tabLayout.setupWithViewPager(vp);
    }
}

