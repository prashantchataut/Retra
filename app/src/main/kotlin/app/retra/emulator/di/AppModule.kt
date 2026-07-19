
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
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): RetraDatabase =
        Room.databaseBuilder(context, RetraDatabase::class.java, "retra.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
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
