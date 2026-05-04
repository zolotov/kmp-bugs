package com.jetbrains.android.kmp

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val content = MainActivity::class.java.getResourceAsStream("/bootstrap/metadata.json").readBytes().decodeToString()
        val textView = TextView(this).apply {
            text = content
            textSize = 32f
            gravity = Gravity.CENTER
        }
        setContentView(textView)
    }
}