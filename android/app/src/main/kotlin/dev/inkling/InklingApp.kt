package dev.inkling

import android.app.Application
import dev.inkling.data.InklingDb
import dev.inkling.data.Repo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class InklingApp : Application() {
    lateinit var repo: Repo
        private set

    override fun onCreate() {
        super.onCreate()
        repo = Repo(InklingDb.open(this))
        instance = this
        // A span open at process start belongs to a session that already ended.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            repo.closeStaleSpans(System.currentTimeMillis())
        }
    }

    companion object {
        lateinit var instance: InklingApp
            private set
    }
}
