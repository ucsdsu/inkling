package dev.inkling

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.inkling.books.BookStore
import dev.inkling.ui.HomeViewModel
import dev.inkling.ui.InklingNav
import dev.inkling.ui.InklingTheme
import dev.inkling.ui.ParentViewModel
import dev.inkling.ui.ReaderViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    /**
     * Counts Home presses. Inkling is the HOME app with launchMode singleTask, so pressing Home
     * re-delivers the intent instead of leaving the activity. Nav watches this to pop to kid home,
     * which disposes the reader route and stops any speech.
     */
    val homePresses = MutableStateFlow(0)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) homePresses.value += 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repo = (application as InklingApp).repo
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
                HomeViewModel::class.java -> HomeViewModel(repo, packageManager) as T
                ParentViewModel::class.java -> ParentViewModel(repo, packageManager, packageName, BookStore(assets)) as T
                ReaderViewModel::class.java -> ReaderViewModel(repo, BookStore(assets), applicationContext) as T
                else -> throw IllegalArgumentException("Unknown ViewModel $modelClass")
            }
        }
        setContent {
            InklingTheme {
                InklingNav(
                    home = viewModel(factory = factory),
                    parent = viewModel(factory = factory),
                    reader = viewModel(factory = factory),
                    homePresses = homePresses,
                )
            }
        }
    }
}
