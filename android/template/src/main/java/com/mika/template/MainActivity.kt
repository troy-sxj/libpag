package com.mika.template

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.mika.template.pag.PAGFileTestActivity
import com.mika.template.pag.PAGPlayerActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun textPAGFile(view: View) {
        startActivity(Intent(this, PAGFileTestActivity::class.java))
    }

    fun textPAGPlayer(view: View) {
        startActivity(Intent(this, PAGPlayerActivity::class.java))
    }
}