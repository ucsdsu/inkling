package dev.inkling

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.inkling.ui.HomeViewModel
import dev.inkling.ui.InklingNav
import dev.inkling.ui.InklingTheme
import dev.inkling.ui.ParentViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repo = (application as InklingApp).repo
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
                HomeViewModel::class.java -> HomeViewModel(repo, packageManager) as T
                ParentViewModel::class.java -> ParentViewModel(repo, packageManager, packageName) as T
                else -> throw IllegalArgumentException("Unknown ViewModel $modelClass")
            }
        }
        setContent {
            InklingTheme {
                InklingNav(home = viewModel(factory = factory), parent = viewModel(factory = factory))
            }
        }
    }
}
