package app.retra.emulator.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.retra.emulator.di.AppModule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RetraDatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        RetraDatabase::class.java
    )

    @Test
    fun migrate5To6_preservesDatabaseAndAddsMetadataColumns() {
        helper.createDatabase(TEST_DATABASE, 5).close()

        helper.runMigrationsAndValidate(
            TEST_DATABASE,
            6,
            true,
            AppModule.MIGRATION_5_6
        ).use { database ->
            database.query("PRAGMA table_info(games)").use { cursor ->
                val nameIndex = cursor.getColumnIndexOrThrow("name")
                val columns = buildSet {
                    while (cursor.moveToNext()) add(cursor.getString(nameIndex))
                }
                assertEquals(true, "sha1" in columns)
                assertEquals(true, "canonicalTitle" in columns)
                assertEquals(true, "metadataSource" in columns)
            }
        }
    }

    private companion object {
        const val TEST_DATABASE = "retra-migration-test"
    }
}
