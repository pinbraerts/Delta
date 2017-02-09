package did.delta;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    private void refreshButton() {
        DBHelper helper = new DBHelper(MainActivity.this);
        Cursor c = helper.getReadableDatabase().rawQuery("select count(*) from " + DBHelper.SAVE_TABLE_NAME, null);
        if(!c.moveToFirst() || c.getInt(0) == 0) findViewById(R.id.buttonContinue).setEnabled(false);
        else findViewById(R.id.buttonContinue).setEnabled(true);
        c.close();
        helper.close();
    }

    @Override
    protected void onResume() {
        refreshButton();
        super.onResume();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.buttonContinue).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, GameActivity.class);
                intent.putExtra("load", true);
                startActivity(intent);
            }
        });
        refreshButton();
        findViewById(R.id.buttonNewGame).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, GameActivity.class));
            }
        });
        findViewById(R.id.buttonExit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
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
