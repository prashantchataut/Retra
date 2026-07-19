
package app.retra.emulator.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [GameEntity::class], version = 3, exportSchema = true)
abstract class RetraDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
}
