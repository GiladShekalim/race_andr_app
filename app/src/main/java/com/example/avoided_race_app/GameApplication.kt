package com.example.avoided_race_app

import android.app.Application
import com.example.avoided_race_app.db.AppDatabase
import org.osmdroid.config.Configuration

class GameApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = packageName
    }
}
