package com.secondmemory.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ThingEntity::class, ActivityEntity::class],
    version = 3,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun thingDao(): ThingDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE things ADD COLUMN notifId INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_things_status ON things (status)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_things_createdAt ON things (createdAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_things_resurfaceAt ON things (resurfaceAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_things_sourceUrl ON things (sourceUrl)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_things_notifId ON things (notifId)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE things ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE things ADD COLUMN pinColor TEXT NOT NULL DEFAULT 'forest'")
                db.execSQL("ALTER TABLE things ADD COLUMN checklist TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE things ADD COLUMN expiresAt INTEGER")
                db.execSQL("ALTER TABLE things ADD COLUMN ogImageUrl TEXT")
            }
        }

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "second-memory.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
    }
}
