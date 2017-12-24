package did.delta

import android.app.AlertDialog
import android.os.Bundle
import did.delta.bases.GameActivityBase

import did.delta.util.DBHelper

class GameActivity : GameActivityBase() {
    override fun wordGuessed(tries: Int) {
        val d = AlertDialog.Builder(this).setNegativeButton(R.string.no) {
            _, _ ->
            finish()
        }.setPositiveButton(R.string.yes) {
            _, _ ->
            startNewGame()
        }.setMessage(getString(R.string.you_won_msg, _keyWord, tries,
                resources.getQuantityString(R.plurals.turns, tries)))
        val hs = helper.getInt(getString(R.string.highscores_table_name), "game_type",
                gameType, "score")
        d.setTitle(if(hs < 0 || hs > tries) {
            helper.setInt(getString(R.string.highscores_table_name), "game_type",
                    gameType, "score", tries)
            R.string.new_record
        } else R.string.you_won_msg)
        d.show()
    }

    private lateinit var helper: DBHelper
    private lateinit var saveTableName: String
    private lateinit var userWordsTableName: String
    private lateinit var wordsTableName: String
    private var save: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        helper = DBHelper(this)
        save = intent.getBooleanExtra("save", false)
        saveTableName = "save_$gameType"
        wordsTableName = "words_$gameType"
        userWordsTableName = "user_words_$gameType"
        super.onCreate(savedInstanceState)
    }

    override fun getWord(): String {
        return if(save) {
            val words = helper.readAll(saveTableName)
            val iter = words.asSequence()
            iter.drop(1).forEach {
                adapter.add(it.word to it.results)
            }
            return words[0].word
        }
        else helper.getRandomWord(wordsTableName)
    }

    override fun checkResult(result: String): Boolean {
        return result == "!!!!"
    }

    override fun save() {
        helper.rewrite(saveTableName, arrayOf(
                mapOf("word" to _keyWord, "description" to "", "results" to ""),
                *(0 until adapter.count).map {
                    val item = adapter.getItem(it)
                    mapOf(
                            "word" to item.first,
                            "description" to "",
                            "results" to item.second)}.toTypedArray()))
    }

    override fun isLegal(word: String): Boolean {
        return helper.contains(wordsTableName, word)
    }

    override fun addNewWord(word: String, description: String) {
        helper.add(userWordsTableName, "word" to word, "description" to description)
    }

    override fun getResult(word: String): String {
        var deltas = 0
        var factorials = 0
        word.forEachIndexed {
            index, c ->
            when(wordSpeeder[c.toInt()]) {
                index.toShort() -> factorials += 1
                (-1).toShort() -> Unit
                else -> deltas += 1
            }
        }
        val sb = StringBuilder()
        (0 until deltas).forEach { sb.append('Δ') }
        (0 until factorials).forEach { sb.append('!') }
        return sb.toString()
    }

}
