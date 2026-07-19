
package app.retra.emulator.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import app.retra.emulator.data.GameDao
import app.retra.emulator.data.RetraDatabase
import app.retra.emulation.api.EmulationCore
import app.retra.emulation.nativecore.MgbaLibretroEmulationCore
import app.retra.emulation.nativecore.NativeReferenceEmulationCore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE games ADD COLUMN origin TEXT NOT NULL DEFAULT 'LOCAL_IMPORT'")
            db.execSQL("ALTER TABLE games ADD COLUMN baseSha256 TEXT")
            db.execSQL("ALTER TABLE games ADD COLUMN patchSha256 TEXT")
            db.execSQL("ALTER TABLE games ADD COLUMN patchFormat TEXT")
            db.execSQL("ALTER TABLE games ADD COLUMN patchDisplayName TEXT")
        }
    }
    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE games ADD COLUMN creator TEXT")
            db.execSQL("ALTER TABLE games ADD COLUMN sourceUrl TEXT")
            db.execSQL("ALTER TABLE games ADD COLUMN license TEXT")
            db.execSQL("ALTER TABLE games ADD COLUMN distributionPermission TEXT")
        }
    }
    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE games ADD COLUMN favorite INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE games ADD COLUMN notes TEXT")
            db.execSQL("ALTER TABLE games ADD COLUMN coverArtPath TEXT")
        }
    }
    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE games ADD COLUMN crc32 INTEGER")
            db.execSQL("ALTER TABLE games ADD COLUMN managedPath TEXT")
            db.execSQL("ALTER TABLE games ADD COLUMN collectionsCsv TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE games ADD COLUMN tagsCsv TEXT NOT NULL DEFAULT ''")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_games_crc32 ON games(crc32)")
        }
    }
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): RetraDatabase =
        Room.databaseBuilder(context, RetraDatabase::class.java, "retra.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .build()

    @Provides
    fun provideGameDao(database: RetraDatabase): GameDao = database.gameDao()

    @Provides
    @Singleton
    fun provideEmulationCore(@ApplicationContext context: Context): EmulationCore {
        val gameplayCore = MgbaLibretroEmulationCore(context)
        return if (gameplayCore.isAvailable) gameplayCore else {
            gameplayCore.close()
            NativeReferenceEmulationCore(context)
        }
    }
}
