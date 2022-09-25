package com.panda912.agpbytecodemanipulationdemo

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    findViewById<Button>(R.id.button).setOnClickListener {
      startActivity(Intent(this@MainActivity, AspectJTestActivity::class.java))
    }
  }
}