package did.delta.bases

import android.app.AlertDialog
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.InputFilter
import android.text.Spanned
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import android.widget.*
import did.delta.R

import did.delta.util.*

abstract class GameActivityBase : AppCompatActivity(), View.OnClickListener,
        View.OnKeyListener {
    abstract fun getResult(word: String) : String
    abstract fun getWord() : String
    abstract fun checkResult(result: String) : Boolean
    abstract fun isLegal(word: String) : Boolean
    abstract fun addNewWord(word: String, description: String)
    abstract fun wordGuessed(tries: Int)
    abstract fun save()

    override fun onKey(v: View?, keyCode: Int, event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER) {
            if (submitButton.isEnabled) submitButton.performClick()
            return true
        }
        return false
    }

    fun startNewGame() {
        keyWord = getWord()
    }

    override fun onClick(it: View) {
        val word = wordBox.text.toString()
        adapter.indexOf(word)?.let {
            toast(getString(R.string.word_is, word))
            findViewById<ListView>(R.id.words_list).setSelection(it)
            return
        }
        if(isLegal(word)) {
            val res = getResult(word)
            adapter.add(word to res)
            wordBox.text = ""
            if(checkResult(res)) {
                wordGuessed(adapter.count)
                adapter.clear()
            }
        }
        else {
            val text = EditText(this)
            text.setHint(R.string.enter_description)
            AlertDialog.Builder(this)
                    .setTitle(getString(R.string.err_no_such_word, word))
                    .setMessage(R.string.add_word_question)
                    .setView(text)
                    .setPositiveButton(R.string.yes) {
                        _, _ -> addNewWord(word, text.text.toString())
                    }
                    .setNegativeButton(R.string.no, null).show()
        }
    }

    protected lateinit var _keyWord: String
    protected var keyWord: String
        get() = _keyWord
        set(value) {
            wordSpeeder.fill((-1).toShort())
            _keyWord = value
            _keyWord.forEachIndexed {
                index, c ->
                wordSpeeder[c.toInt()] = index.toShort()
            }
        }
    protected var wordSpeeder = Array('\ufffe'.toInt(), { (-1).toShort() })
    protected lateinit var adapter: WordsListAdapter
    protected lateinit var submitButton: Button
    protected lateinit var wordBox: TextView
    protected val gameType: String by lazy {
        intent.getStringExtra("game_type") ?: "normal"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_base)

        setSupportActionBar(findViewById(R.id.gameToolbar))
        supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setDisplayShowHomeEnabled(true)
        }

        adapter = WordsListAdapter(this)
        val list = findViewById<ListView>(R.id.words_list)
        list.adapter = adapter
        list.transcriptMode = ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL

        submitButton = findViewById(R.id.buttonSubmit)
        submitButton.isEnabled = false
        submitButton.setOnClickListener(this)

        wordBox = findViewById(R.id.wordBox)
        val filter = filters[gameType]
        filter?.let {
            wordBox.filters = arrayOf(InputFilter { p1, p2, p3, p4, p5, p6 ->
                val (en, res) = it(p1, p2, p3, p4, p5, p6)
                submitButton.isEnabled = en
                res
            })
        }
        wordBox.setOnKeyListener(this)

        startNewGame()
    }

    override fun onBackPressed() {
        save()
        super.onBackPressed()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) onBackPressed()
        return super.onOptionsItemSelected(item)
    }
}
