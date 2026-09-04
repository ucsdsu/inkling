package dev.inkling

import android.app.Application
import dev.inkling.data.InklingDb
import dev.inkling.data.Repo

class InklingApp : Application() {
    lateinit var repo: Repo
        private set

    override fun onCreate() {
        super.onCreate()
        repo = Repo(InklingDb.open(this))
        instance = this
    }

    companion object {
        lateinit var instance: InklingApp
            private set
    }
}
