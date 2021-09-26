package com.shevelev.page_turning_test_app

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.startFirst).setOnClickListener {
            CurlActivity.start(this, 0)
        }

        findViewById<Button>(R.id.startThird).setOnClickListener {
            CurlActivity.start(this, 2)
        }
    }
}