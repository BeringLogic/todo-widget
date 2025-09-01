package com.example.todowidget

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.todowidget.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // In a real app, you would set up your main activity UI here
        supportActionBar?.title = getString(R.string.app_name)
    }
}
