package com.kaz.midnight

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS index_DreamTagCrossRef_tagId ON DreamTagCrossRef(tagId)")
    }
}
