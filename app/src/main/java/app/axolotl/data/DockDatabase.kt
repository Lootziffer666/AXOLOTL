package app.axolotl.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

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
                val applicationContext = context.applicationContext
                val passphrase = DatabasePassphraseStore(applicationContext).getOrCreate()
                PlaintextDatabaseMigrator.migrateIfNeeded(
                    applicationContext.getDatabasePath(DATABASE_NAME),
                    passphrase
                )
                val instance = Room.databaseBuilder(
                    applicationContext,
                    DockDatabase::class.java,
                    DATABASE_NAME
                )
                .openHelperFactory(SupportOpenHelperFactory(passphrase))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private const val DATABASE_NAME = "dock_database"
    }
}
