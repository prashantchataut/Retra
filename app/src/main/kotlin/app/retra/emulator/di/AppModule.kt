
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
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE games ADD COLUMN origin TEXT NOT NULL DEFAULT 'LOCAL_IMPORT'")
            db.execSQL("ALTER TABLE games ADD COLUMN baseSha256 TEXT")
            db.execSQL("ALTER TABLE games ADD COLUMN patchSha256 TEXT")
            db.execSQL("ALTER TABLE games ADD COLUMN patchFormat TEXT")
            db.execSQL("ALTER TABLE games ADD COLUMN patchDisplayName TEXT")
        }
    }
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE games ADD COLUMN creator TEXT")
            db.execSQL("ALTER TABLE games ADD COLUMN sourceUrl TEXT")
            db.execSQL("ALTER TABLE games ADD COLUMN license TEXT")
            db.execSQL("ALTER TABLE games ADD COLUMN distributionPermission TEXT")
        }
    }
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE games ADD COLUMN favorite INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE games ADD COLUMN notes TEXT")
            db.execSQL("ALTER TABLE games ADD COLUMN coverArtPath TEXT")
        }
    }
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE games ADD COLUMN crc32 INTEGER")
            db.execSQL("ALTER TABLE games ADD COLUMN managedPath TEXT")
            db.execSQL("ALTER TABLE games ADD COLUMN collectionsCsv TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE games ADD COLUMN tagsCsv TEXT NOT NULL DEFAULT ''")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_games_crc32 ON games(crc32)")
        }
    }
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE games ADD COLUMN sha1 TEXT")
            db.execSQL("ALTER TABLE games ADD COLUMN canonicalTitle TEXT")
            db.execSQL("ALTER TABLE games ADD COLUMN metadataSource TEXT")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_games_sha1 ON games(sha1)")
        }
    }
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): RetraDatabase =
        Room.databaseBuilder(context, RetraDatabase::class.java, "retra.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
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
