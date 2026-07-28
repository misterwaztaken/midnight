package com.kaz.midnight

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migrations {

    // version 1 was never released, so no MIGRATION_1_2 is needed

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // add migration sql here when the schema changes
            // example: db.execSQL("ALTER TABLE dreams ADD COLUMN newColumn TEXT NOT NULL DEFAULT ''")
        }
    }
}
