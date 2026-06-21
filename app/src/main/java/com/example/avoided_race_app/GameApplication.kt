package com.example.avoided_race_app

import android.app.Application
import com.example.avoided_race_app.db.AppDatabase

class GameApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
}
