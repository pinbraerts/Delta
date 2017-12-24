package did.delta.util

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import java.io.InputStreamReader

import did.delta.R
import did.delta.bases.WordsListRow

class DBHelper(private val context: Context, version: Int) :
        SQLiteOpenHelper(context, context.getString(R.string.db_name), null, version) {

    fun contains(tableName: String, word: String) : Boolean {
        val c = readableDatabase
                .rawQuery(context.getString(R.string.cmd_count_entrys, tableName, word),
                        null)
        val res = c.moveToFirst() && c.getInt(0) > 0
        c.close()
        close()
        return res
    }

    fun delete(tableName: String, column: String, value: String) {
        writableDatabase.delete(tableName, "$column=?", arrayOf(value))
        close()
    }

    fun setInt(tableName: String, cmpcolumn: String, cmpval: String, column: String, value: Int) {
        try {
            val cv = ContentValues()
            cv.put(column, value)
            writableDatabase.update(tableName, cv, "$column = ?", arrayOf(cmpval))
        } finally {
            close()
        }
    }

    fun rewrite(tableName: String, columns: Array<Map<String, String>>) {
        try {
            val wd = writableDatabase
            wd.execSQL(context.getString(R.string.cmd_delete, tableName))
            wd.execSQL("vacuum")

            columns.forEach { column ->
                val cv = ContentValues()
                column.forEach { (k, v) -> cv.put(k, v) }
                wd.insertOrThrow(tableName, null, cv)
            }
        } finally {
            close()
        }
    }

    fun getInt(tableName: String, cmpcolumn: String, cmpval: String,
               rescolumn: String, default: Int = -1) : Int {
        val c = readableDatabase.
                rawQuery(context.getString(R.string.cmd_usr_query, tableName,
                        cmpcolumn, cmpval, rescolumn),
                        null)
        var res = default
        if (c.moveToFirst()) res = c.getInt(c.getColumnIndex(rescolumn))
        c.close()
        close()
        return res
    }

    fun empty(tableName: String): Boolean {
        val c = readableDatabase.
                rawQuery(context.getString(R.string.cmd_count, tableName), null)
        val res = c.moveToFirst() && c.getInt(0) != 0
        c.close()
        close()
        return res
    }

    fun add(tableName: String, vararg column: Pair<String, String>) {
        val cv = ContentValues()
        column.forEach { (k, v) -> cv.put(k, v) }
        try {
            writableDatabase.insertOrThrow(tableName, null, cv)
        } finally {
            close()
        }
    }

    fun readAll(tableName: String): ArrayList<WordsListRow> {
        val res = ArrayList<WordsListRow>()
        val c = readableDatabase.rawQuery(context.getString(R.string.cmd_full_query, tableName), null)
        if(c.moveToFirst()) do res.add(WordsListRow(c.getString(c.getColumnIndex("word")),
                c.getString(c.getColumnIndex("description")),
                c.getString(c.getColumnIndex("results")))) while(c.moveToNext())
        c.close()
        close()
        return res
    }

    fun readAll2(tableName: String): ArrayList<Pair<String, String>> {
        val res = ArrayList<Pair<String, String>>()
        val c = readableDatabase.rawQuery(context.getString(R.string.cmd_full_query, tableName), null)
        if(c.moveToFirst()) do res.add(c.getString(c.getColumnIndex("word")) to
                c.getString(c.getColumnIndex("description"))) while(c.moveToNext())
        c.close()
        close()
        return res
    }

    fun getRandomWord(tableName: String) : String {
        val c = readableDatabase.rawQuery(context.getString(R.string.cmd_get_random, tableName), null)
        var res: String? = null
        if(c.moveToFirst()) res = c.getString(c.getColumnIndex("word"))!!
        c.close()
        close()
        return res ?: throw Exception("Database is empty!")
    }

    constructor(context: Context) : this(context, 1)

    val gameTypes: Array<out String> = context.resources.getStringArray(R.array.game_types)!!

    override fun onCreate(db: SQLiteDatabase) {
        try {
            db.execSQL(context.getString(R.string.cmd_create_table_highscores,
                    context.getString(R.string.highscores_table_name)))

            gameTypes.forEach {
                game_type ->
                val cv = ContentValues()
                cv.put("game_type", game_type)
                cv.put("score", -1)
                db.insertOrThrow(context.getString(R.string.highscores_table_name),
                        null, cv)

                context.resources.getStringArray(R.array.table_types).forEach {
                    table_type ->
                    db.execSQL(context.getString(R.string.cmd_create_table, table_type, game_type))
                }

                db.execSQL(context.getString(R.string.cmd_create_save_table, game_type))

                val tableName = "words_$game_type"
                val fileReader = InputStreamReader(context.assets.open("$tableName.txt"))
                fileReader.forEachLine {
                    val row = it.split('|')
                    val contentValues = ContentValues()
                    contentValues.put("word", row[0])
                    contentValues.put("description", row.getOrElse(1, { "" }))
                    db.insertOrThrow(tableName, null, contentValues)
                }
            }
        } catch (e: Exception) {
//            context.toast(e.localizedMessage)
            Log.e("did.delta.util.DBHelper", e.localizedMessage)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, i: Int, i1: Int) {
        gameTypes.forEach {
            db.execSQL(context.getString(R.string.cmd_delete_table, "words_$it"))
        }
        onCreate(db)
    }
}
