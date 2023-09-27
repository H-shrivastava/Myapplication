package com.example.myapplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import com.example.myapplication.sample.module1.module1String
import com.example.myapplication.sample.module2.module2String

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(TextView(this).apply {
            text = """
                Hey there! Let's do some math!
                 $module1String
                 $module2String
            """.trimIndent()
        })
    }
}