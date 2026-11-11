package com.creditlens.app

import android.app.Application
import com.creditlens.app.data.local.CreditLensDatabase
import com.creditlens.app.data.repository.CreditLensRepository

class CreditLensApplication : Application() {

    val database by lazy { CreditLensDatabase.getDatabase(this) }
    val repository by lazy { CreditLensRepository(database.assessmentDao(), applicationContext) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: CreditLensApplication
            private set
    }
}
