package did.delta;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class DBHelper extends SQLiteOpenHelper {

    static public String WORDS_TABLE_NAME = "words";
    static public String SAVE_TABLE_NAME = "save";
    private Context mContext;

    public DBHelper(Context context) {
        super(context, context.getString(R.string.db_name), null, 1);
        mContext = context;
    }

    public DBHelper(Context context, int version) {
        super(context, context.getString(R.string.db_name), null, version);
        mContext = context;
    }

    @RequiresApi(api = Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table " + WORDS_TABLE_NAME + " (word varchar(4), description varchar(255), accepted integer);");
        db.execSQL("create table " + SAVE_TABLE_NAME + " (word varchar(4), info varchar(4));");

        try {
            BufferedReader fileReader = new BufferedReader(new InputStreamReader(mContext.getResources().openRawResource(R.raw.words)));
            String[] temwords;
            String temword;

            for (int i = 0; (temword = fileReader.readLine()) != null; ++i) {
                temwords = temword.split(" ");
                ContentValues cv = new ContentValues();
                cv.put("word", temwords[0]);
                cv.put("description", temwords.length > 1 ? temwords[1] : "null");
                cv.put("accepted", temwords.length > 2 ? temwords[2] : "1");
                db.insertOrThrow(WORDS_TABLE_NAME, null, cv);
            }
            fileReader.close();
        } catch (Exception e) {
            Toast.makeText(mContext, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        db.execSQL("drop table if exists " + WORDS_TABLE_NAME + ';');
        db.execSQL("drop table if exists " + SAVE_TABLE_NAME + ';');
        onCreate(db);
    }
}
