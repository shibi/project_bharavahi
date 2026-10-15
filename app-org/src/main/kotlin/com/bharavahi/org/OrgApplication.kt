package com.bharavahi.org

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.bharavahi.org.sheets.WorkManagerSheetSyncScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/** Gives WorkManager Hilt's worker factory, so the backup workers get their dependencies. */
@HiltAndroidApp
class OrgApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var sheetSync: WorkManagerSheetSyncScheduler

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        sheetSync.schedulePeriodic()
    }
}
