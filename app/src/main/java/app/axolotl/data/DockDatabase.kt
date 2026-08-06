package app.axolotl.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        DockAppEntity::class,
        DockItemEntity::class,
        SnippetEntity::class,
        ClipEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class DockDatabase : RoomDatabase() {
    abstract fun dockAppDao(): DockAppDao
    abstract fun dockItemDao(): DockItemDao
    abstract fun snippetDao(): SnippetDao
    abstract fun clipDao(): ClipDao

    companion object {
        @Volatile
        private var INSTANCE: DockDatabase? = null

        fun getDatabase(context: Context): DockDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DockDatabase::class.java,
                    "dock_database"
                )
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
