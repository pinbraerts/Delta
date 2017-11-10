package did.delta

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View

import did.delta.util.DBHelper

class MainActivity : AppCompatActivity() {

    private fun refreshButton() {
        // TODO: rewrite with many game types and buttons
        DBHelper(this@MainActivity).empty("save_normal")
    }

    override fun onResume() {
        refreshButton()
        super.onResume()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<View>(R.id.buttonContinue).setOnClickListener {
            val intent = Intent(this@MainActivity, GameActivity::class.java)
            intent.putExtra("save", true)
            startActivity(intent)
        }
        refreshButton()
        findViewById<View>(R.id.buttonNewGame).setOnClickListener {
            startActivity(Intent(this@MainActivity, GameActivity::class.java))
        }
        findViewById<View>(R.id.buttonWordsList).setOnClickListener {
            startActivity(Intent(this@MainActivity, ListActivity::class.java))
        }
        findViewById<View>(R.id.buttonExit).setOnClickListener {
            finish()
        }
        Thread {
            val helper = DBHelper(this@MainActivity)
            helper.getWritableDatabase()
            helper.close()
        }.start()
    }

}
