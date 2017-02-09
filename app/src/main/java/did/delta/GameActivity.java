package did.delta;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

public class GameActivity extends AppCompatActivity
    implements View.OnClickListener, InputFilter, View.OnKeyListener, DialogInterface.OnCancelListener {

    @Override
    public CharSequence filter(CharSequence charSequence, int i, int i1,
                               Spanned spanned, int i2, int i3) {
        int len = i1 - i + wordBox.length();
        if(charSequence.toString().matches("[а-я,ё]")
                && len <= 4
                && (charSequence.length() == 0
                || wordBox.getText()
                        .toString()
                        .indexOf(charSequence.charAt(charSequence.length() - 1)) == -1)
                ) {
            enterButton.setEnabled(len == 4);
            return null;
        }
        enterButton.setEnabled(wordBox.length() == 4);
        return "";
    }

    @Override
    public void onBackPressed() {
        saveGame();
        super.onBackPressed();
    }

    @Override
    public void onCancel(DialogInterface dialogInterface) {
        onBackPressed();
    }

    public interface WordGuessedListener {
        void onWordGuessedListener(int turns);
    }

    private void saveGame() {
        try {
            SQLiteDatabase db = helper.getWritableDatabase();
            db.execSQL("delete from " + DBHelper.SAVE_TABLE_NAME);
            db.execSQL("vacuum");
            if(!gameEnd) {
                ContentValues cv = new ContentValues();
                cv.put("word", word);
                db.insertOrThrow(DBHelper.SAVE_TABLE_NAME, null, cv);
                for (String string : adapter.array) {
                    cv = new ContentValues();
                    cv.put("word", string);
                    db.insertOrThrow(DBHelper.SAVE_TABLE_NAME, null, cv);
                }
            }
        } finally {
            helper.close();
        }
    }

    private class WordAdapter extends ArrayAdapter<String> {
        public WordAdapter(Context ctx, int id, ArrayList<String> arr) {
            super(ctx, id, arr);
            array = new ArrayList<>(arr);
        }

        private ArrayList<String> array;

        @Override
        @NonNull
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            LinearLayout layout = new LinearLayout(getContext());
            String string = getItem(position);
            layout.setOrientation(LinearLayout.HORIZONTAL);
            TextView textView = new TextView(getContext());
            assert string != null;
            textView.setText(string.substring(0, 4));
            textView.setTextSize(30);
            textView.setTextColor(getColor(R.color.textColorPrimary));
            layout.addView(textView);
            if(string.length() > 4) {
                TextView deltas = new TextView(getContext());
                deltas.setText(string.substring(4));
                deltas.setTextSize(30);
                deltas.setTextColor(getColor(R.color.textColorPrimary));
                deltas.setGravity(Gravity.END);
                deltas.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        100));
                layout.addView(deltas);
            }
            return layout;
        }

        @Override
        public void add(String string) {
            array.add(string);
            int deltas = 0, factorials = 0;
            for(int i = 0; i < string.length(); ++i) {
                int chr = string.charAt(i) == 'ё' ? 7 : string.charAt(i) - 'а';
                if(wordChars[chr])
                    if(string.charAt(i) == word.charAt(i)) ++factorials;
                    else ++deltas;
            }
            if(factorials != 0 || deltas != 0) {
                for(int i = 0; i < factorials; ++i) string += '!';
                for(int i = 0; i < deltas; ++i) string += 'Δ';
            }
            super.add(string);
            if(factorials == 4) wordListener.onWordGuessedListener(array.size());
            wordBox.setText("");
        }

        @Override
        public void clear() {
            array.clear();
            super.clear();
        }

        public int indexOf(String s) {
            return array.indexOf(s);
        }
    }

    private WordAdapter adapter;
    private TextView wordBox;
    private String word;
    private boolean wordChars[] = new boolean[33];
    private boolean gameEnd = false;
    private DBHelper helper;
    private DialogInterface.OnClickListener endOfGame = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            gameEnd = true;
            switch (i) {
                case DialogInterface.BUTTON_POSITIVE:
                    adapter.clear();
                    wordBox.setText("");
                    word = getRandomWord();
                    parceWord();
                    break;
                case DialogInterface.BUTTON_NEGATIVE:
                    GameActivity.this.onBackPressed();
                    break;
            }
        }
    };
    private WordGuessedListener wordListener = new WordGuessedListener() {
        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
        @Override
        public void onWordGuessedListener(int turns) {
            SharedPreferences pref = getPreferences(MODE_PRIVATE);
            int highscore = pref.getInt("highscore", -1);
            AlertDialog.Builder dialog =
                    new AlertDialog.Builder(GameActivity.this)
                    .setMessage("Вы отгадали слово \"" + word + "\" за "
                            + turns + " ход" + getStringObject(turns) + "!\nНачать новую игру?")
                    .setPositiveButton("Да", endOfGame)
                    .setNegativeButton("Нет", endOfGame)
                    .setOnCancelListener(GameActivity.this);
            if(highscore < 0 || turns < highscore) {
                SharedPreferences.Editor editor = pref.edit();
                editor.putInt("highscore", turns);
                editor.apply();
                dialog.setTitle("Новый личный рекорд!");
            } else dialog.setTitle("Победа!");
            dialog.show();
        }
    };
    private Button enterButton;

    private String getStringObject(int turns) {
        switch(turns % 10) {
            case 1:
                return "";
            case 2: case 3: case 4:
                return "а";
            default:
                return "ов";
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        helper = new DBHelper(this);

        setContentView(R.layout.activity_game);

        adapter = new WordAdapter(this,
                android.R.layout.simple_expandable_list_item_1,
                new ArrayList<String>());
        enterButton = (Button) findViewById(R.id.buttonEnter);
        enterButton.setOnClickListener(this);
        enterButton.setEnabled(false);
        ListView wordsLayout = (ListView) findViewById(R.id.wordsList);
        wordsLayout.setAdapter(adapter);
        wordsLayout.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        wordBox = (TextView) findViewById(R.id.wordBox);
        wordBox.setFilters(new InputFilter[] { this });
        wordBox.setOnKeyListener(this);

        if(getIntent().getBooleanExtra("load", false)) {
            Cursor c = helper.getReadableDatabase().rawQuery("select * from " + DBHelper.SAVE_TABLE_NAME, null);
            if(c.moveToFirst()) {
                word = c.getString(c.getColumnIndex("word"));
                parceWord();
                while(c.moveToNext()) adapter.add(c.getString(c.getColumnIndex("word")));
            }
            c.close();
            helper.close();
        } else {
            word = getRandomWord();
            parceWord();
        }
    }

    private void parceWord() {
        // Toast.makeText(this, word, Toast.LENGTH_LONG).show();
        for(int i = 0; i < 33; ++i) wordChars[i] = false;
        for(int i = 0; i < word.length(); ++i) {
            int chr = word.charAt(i) == 'ё' ? 7 : word.charAt(i) - 'а';
            wordChars[chr] = true;
        }
    }

    private String getRandomWord() {
        try {
            SQLiteDatabase db = helper.getReadableDatabase();
            Cursor c = db.rawQuery("select word from " + DBHelper.WORDS_TABLE_NAME +
                    " where accepted = 1 order by RANDOM() limit 1", null);
            if(c.moveToFirst()) return c.getString(c.getColumnIndex("word"));
            c.close();
            Toast.makeText(this, "Error: word database is empty!", Toast.LENGTH_SHORT).show();
            return "душа";
        } catch (Exception e) {
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            return "душа";
        } finally {
            helper.close();
        }
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event)
    {
        if (event.getAction() == KeyEvent.ACTION_DOWN
                && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
            if(wordBox.length() > 3) enterButton.performClick();
            return true;
        }
        return false;
    }

    @Override
    public void onClick(View view) {
        String temword = wordBox.getText().toString();
        int index = adapter.indexOf(temword);
        if(index != -1) {
            Toast.makeText(this, "Слово \"" + temword + "\" уже было!", Toast.LENGTH_SHORT).show();
            ((ListView) findViewById(R.id.wordsList)).setSelection(index);
            return;
        }
        try {
            Cursor c = helper.getReadableDatabase()
                    .rawQuery("select count(*) from " + DBHelper.WORDS_TABLE_NAME +
                            " where word = ?", new String[]{temword});
            if (c.moveToFirst() && c.getInt(0) > 0) adapter.add(wordBox.getText().toString());
            else Toast.makeText(this, "Слова \"" + temword + "\" не существует в списке слов!", Toast.LENGTH_SHORT).show();
            c.close();
        } finally {
            helper.close();
        }
    }
}
