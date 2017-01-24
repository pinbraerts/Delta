package did.delta;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

public class GameActivity extends AppCompatActivity
    implements View.OnClickListener, InputFilter, View.OnKeyListener { // Эта строчка позволяет Java рассматривать эту страницу и как обработчик нажатия на кнопку)

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

    public interface WordGuessedListener {
        void onWordGuessedListener();
    }

    private class WordAdapter extends ArrayAdapter<String> { // Эта штуковина отвечает за отображение слова и за операции, связанные с жобавлением слова)
        public WordAdapter(Context ctx, int id, ArrayList<String> arr) {
            super(ctx, id, arr);
            array = new ArrayList<>(arr);
        }

        private ArrayList<String> array;

        @Override
        @NonNull
        public View getView(int position, View convertView, @NonNull ViewGroup parent) { // Отображение листа)
            LinearLayout layout = new LinearLayout(getContext());
            String string = getItem(position);
            layout.setOrientation(LinearLayout.HORIZONTAL);
            TextView textView = new TextView(getContext());
            assert string != null;
            textView.setText(string.substring(0, 4)); // Отображаю слово)
            textView.setTextSize(30);
            textView.setTextColor(getColor(R.color.textColorPrimary));
            layout.addView(textView);
            if(string.length() > 4 && string.charAt(4) == ',') {
                TextView deltas = new TextView(getContext());
                deltas.setText(string.substring(5));
                deltas.setTextSize(30);
                deltas.setTextColor(getColor(R.color.textColorPrimary));
                layout.addView(deltas);
            }
            return layout;
        }

        @Override
        public void add(String string) {
            array.add(string);
            int deltas = 0, factorials = 0;
            for(int i = 0; i < string.length(); ++i) {
                int chr = string.charAt(i) - 'а'; // Русская)
                if(wordChars[chr]) // Массив с true на местах, соответствующим буквам искомого слова)
                    if(string.charAt(i) == word.charAt(i)) ++factorials; // Прямое попадание)
                    else ++deltas;
            }
            if(factorials != 0 || deltas != 0) {
                string += ",";
                for(int i = 0; i < factorials; ++i) string += '!';
                for(int i = 0; i < deltas; ++i) string += 'Δ';
            }
            super.add(string);
            if(factorials == 4) wordListener.onWordGuessedListener(); // Выполняется то, что должно быть после угадывания слова)
            wordBox.setText("");
        }

        public int indexOf(String s) {
            return array.indexOf(s);
        }
    }

    private WordAdapter adapter;
    private TextView wordBox;
    private String word;
    private HashSet<String> words = new HashSet<>();
    private boolean wordChars[] = new boolean[33];
    private WordGuessedListener wordListener = new WordGuessedListener() {
        @Override
        public void onWordGuessedListener() {
            Toast.makeText(getApplicationContext(), "Угадал!", Toast.LENGTH_SHORT).show();
        }
    };
    private Button enterButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            BufferedReader fileReader = new BufferedReader(new InputStreamReader(getResources().openRawResource(R.raw.words)));
            int last = new Random().nextInt(756);
            String temword;
            for(int i = 0; (temword = fileReader.readLine()) != null; ++i) {
                if(i == last) word = temword;
                words.add(temword);
            }
            fileReader.close();
        } catch (IOException e) {
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            word = "душа";
        }
        for(int i = 0; i < word.length(); ++i) {
            int chr = word.charAt(i) - 'а'; // Русская)
            wordChars[chr] = true;
        }

        setContentView(R.layout.activity_game);

        adapter = new WordAdapter(this,
                android.R.layout.simple_expandable_list_item_1,
                new ArrayList<String>());
        enterButton = (Button) findViewById(R.id.buttonEnter);
        enterButton.setOnClickListener(this); // this -- текущий объект-странца, который может быть и обработчиком)
        enterButton.setEnabled(false); // Пока выключаем кнопку)
        ListView wordsLayout = (ListView) findViewById(R.id.wordsList);
        wordsLayout.setAdapter(adapter);
        wordsLayout.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        wordBox = (TextView) findViewById(R.id.wordBox);
        wordBox.setFilters(new InputFilter[] { this });
        wordBox.setOnKeyListener(this);
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event)
    {
        if (event.getAction() == KeyEvent.ACTION_DOWN
                && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
            enterButton.performClick();
            return true;
        }
        return false;
    }

    @Override
    public void onClick(View view) { // Сам обработчик)
        String temword = wordBox.getText().toString();
        int index = adapter.indexOf(temword);
        if(index != -1) {
            Toast.makeText(this, "Слово \"" + temword + "\" уже было!", Toast.LENGTH_SHORT).show();
            ((ListView) findViewById(R.id.wordsList)).setSelection(index);
            return;
        }
        if(words.contains(temword)) adapter.add(wordBox.getText().toString());
        else Toast.makeText(this, "Слова \"" + temword + "\" не существует!", Toast.LENGTH_SHORT).show();
    }
}
