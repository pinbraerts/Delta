package did.delta;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.support.annotation.NonNull;
import android.app.AlertDialog;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;

import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

public class GameActivity extends AppCompatActivity
    implements View.OnClickListener, InputFilter, View.OnKeyListener, DialogInterface.OnCancelListener { // Эта строчка позволяет Java рассматривать эту страницу и как обработчик нажатия на кнопку)

    @Override
    public CharSequence filter(CharSequence charSequence, int i, int i1, Spanned spanned, int i2, int i3) { // Этот метод проверяет текст на правильность)
        if(charSequence.toString().matches("[а-я,ё]") // Это значит, что строка состоит из русских букв и короче 5)
                && i2 < 4
                && (charSequence.length() == 0
                || wordBox.getText()
                        .toString()
                        .indexOf(charSequence.charAt(charSequence.length() - 1)) == -1) // Если в предыдущей строке нет такого же символа)
                ) {
            enterButton.setEnabled(i2 == 3); // Если слово достаточно длинное, то можно и включить кнопку)
            return null; // Можно вернуть null или строку, если null, то срока правильная, а если какая-нибудь строка, то она и становится текстом в окошке)
        }
        enterButton.setEnabled(i2 == 4); // А тут выключаем, если текущее слово не длинное)
        return "";
    }

    @Override
    public void onCancel(DialogInterface dialogInterface) {
        try {
            FileOutputStream fileWriter = openFileOutput("highscore.txt", MODE_PRIVATE);
            fileWriter.write(highscore + '0');
            fileWriter.close();
        } catch (IOException e) {
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        }
        onBackPressed();
    }

    public interface WordGuessedListener {
        void onWordGuessedListener(int turns);
    }

    private class DBHelper extends SQLiteOpenHelper {

        public DBHelper(Context context) {
            super(context, getString(R.string.db_name), null, 1);
        }

        public DBHelper(Context context, int version) {
            super(context, getString(R.string.db_name), null, version);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("create table " + getString(R.string.db_name) + "(word varchar(4), description varchar(255), accepted integer);");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int i, int i1) {
            db.execSQL("drop table if exists " + getResources().getString(R.string.db_name) + ';');
            onCreate(db);
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
            textView.setTextColor(getColor(R.color.redTextColorPrimary));
            layout.addView(textView);
            if(string.length() > 4) {
                TextView deltas = new TextView(getContext());
                deltas.setText(string.substring(4));
                deltas.setTextSize(30);
                deltas.setTextColor(getColor(R.color.redTextColorPrimary));
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
    private DBHelper helper;
    private DialogInterface.OnClickListener endOfGame = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            switch (i) {
                case DialogInterface.BUTTON_POSITIVE:
                    adapter.clear();
                    wordBox.clearComposingText();
                    word = getRandomWord();
                    parceWord();
                    break;
                case DialogInterface.BUTTON_NEGATIVE:
                    try {
                        FileOutputStream fileWriter = openFileOutput("highscore.txt", MODE_PRIVATE);
                        fileWriter.write(highscore + '0');
                        fileWriter.close();
                    } catch (IOException e) {
                        Toast.makeText(GameActivity.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };
    private WordGuessedListener wordListener = new WordGuessedListener() {
        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
        @Override
        public void onWordGuessedListener(int turns) {
            AlertDialog.Builder dialog =
                    new AlertDialog.Builder(GameActivity.this)
                    .setMessage("Вы отгадали слово \"" + word + "\" за " + turns + " ход" + getStringObject(turns) + "!\nНачать новую игру?")
                    .setPositiveButton("Да", endOfGame)
                    .setNegativeButton("Нет", endOfGame)
                    .setOnCancelListener(GameActivity.this);
            if(turns > highscore) {
                highscore = turns;
                dialog.setTitle("Новый личный рекорд!");
            }
            else dialog.setTitle("Победа!");
            dialog.show();
        }
    };
    private Button enterButton;
    private int highscore = 0;

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
        SQLiteDatabase database = helper.getReadableDatabase();
        try {
            Cursor c = database.rawQuery("select count(*) from words", null);
            c.moveToFirst();
            if(c.getInt(0) == 0) {
                c.close();
                helper.close();
                try {
                    database = helper.getWritableDatabase();
                    BufferedReader fileReader = new BufferedReader(new InputStreamReader(getResources().openRawResource(R.raw.words)));
                    String temword;

                    for (int i = 0; (temword = fileReader.readLine()) != null; ++i) {
                        ContentValues cv = new ContentValues();
                        cv.put("word", temword);
                        cv.put("description", "NULL");
                        cv.put("accepted", 1);
                        database.insertOrThrow(getString(R.string.db_name), null, cv);
                    }
                    fileReader.close();
                } catch (SQLiteException e) {
                    Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    word = "душа";
                } finally {
                    helper.close();
                }
            } else c.close();
            helper.close();
            word = getRandomWord();
        } catch (SQLiteException e) {
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            helper.close();
        }
        // word = "душа";
        parceWord();

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
    }

    private void parceWord() {
        for(int i = 0; i < 33; ++i) wordChars[i] = false;
        for(int i = 0; i < word.length(); ++i) {
            int chr = word.charAt(i) == 'ё' ? 7 : word.charAt(i) - 'а';
            wordChars[chr] = true;
        }
    }

    private String getRandomWord() {
        try {
            SQLiteDatabase db = helper.getReadableDatabase();
            Cursor c = db.rawQuery("select word from words where accepted = 1 order by RANDOM() limit 1;", null);
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
                    .rawQuery("select word from words where word = ?", new String[]{temword});
            if (c.moveToFirst()) adapter.add(wordBox.getText().toString());
            else Toast.makeText(this, "Слова \"" + temword + "\" не существует!", Toast.LENGTH_SHORT).show();
            c.close();
        } finally {
            helper.close();
        }
    }
}
