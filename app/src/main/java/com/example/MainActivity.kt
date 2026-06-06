package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.data.db.JarvisDatabase
import com.example.data.repository.JarvisRepository
import com.example.ui.screens.JarvisMainScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.JarvisViewModel
import com.example.ui.viewmodel.JarvisViewModelFactory

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    val database = JarvisDatabase.getDatabase(applicationContext)
    val repository = JarvisRepository(database)
    val factory = JarvisViewModelFactory(repository)
    val viewModel = ViewModelProvider(this, factory)[JarvisViewModel::class.java]

    setContent {
      JarvisMainScreen(viewModel = viewModel)
    }
  }
}
