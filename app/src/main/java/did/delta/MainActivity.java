package did.delta;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button bcontinue = (Button)findViewById(R.id.buttonContinue);
        DBHelper helper = new DBHelper(MainActivity.this);
        Cursor c = helper.getReadableDatabase().rawQuery("select count(*) from " + DBHelper.SAVE_TABLE_NAME, null);
        if(!c.moveToFirst() || c.getInt(0) == 0) bcontinue.setEnabled(false);
        c.close();
        helper.close();
        bcontinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, GameActivity.class);
                intent.putExtra("load", true);
                startActivity(intent);
            }
        });
        findViewById(R.id.buttonNewGame).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, GameActivity.class));
            }
        });
        findViewById(R.id.buttonExit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.this.onBackPressed();
            }
        });

        new Thread() {
            @Override
            public void run() {
                DBHelper helper = new DBHelper(MainActivity.this);
                helper.getWritableDatabase();
                helper.close();
            }
        }.start();
    }
}
