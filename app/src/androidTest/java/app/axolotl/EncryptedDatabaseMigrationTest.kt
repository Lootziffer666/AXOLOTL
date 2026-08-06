package app.axolotl

import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.axolotl.data.PlaintextDatabaseMigrator
import app.axolotl.data.DatabasePassphraseStore
import net.zetetic.database.sqlcipher.SQLiteDatabase as CipherDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class EncryptedDatabaseMigrationTest {
    @Test
    fun passphraseIsStableAndKeystoreWrapped() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        context.getSharedPreferences(DatabasePassphraseStore.PREFERENCES_NAME, 0)
            .edit().clear().commit()
        val store = DatabasePassphraseStore(context)

        val first = store.getOrCreate()
        val second = store.getOrCreate()

        assertEquals(32, first.size)
        assertTrue(first.contentEquals(second))
        assertFalse(
            context.getSharedPreferences(DatabasePassphraseStore.PREFERENCES_NAME, 0)
                .all.values.any { it == android.util.Base64.encodeToString(first, android.util.Base64.NO_WRAP) }
        )
    }

    @Test
    fun plaintextDatabaseIsEncryptedWithoutLosingRows() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val directory = File(context.cacheDir, "sqlcipher-migration").apply { mkdirs() }
        val file = File(directory, "legacy.db").apply { delete() }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { database ->
            database.execSQL("CREATE TABLE sample (value TEXT NOT NULL)")
            database.execSQL("INSERT INTO sample VALUES ('preserved')")
        }
        val passphrase = ByteArray(32) { it.toByte() }

        try {
            PlaintextDatabaseMigrator.migrateIfNeeded(file, passphrase)

            assertFalse(PlaintextDatabaseMigrator.hasPlaintextHeader(file))
            CipherDatabase.openDatabase(
                file.path,
                passphrase,
                null,
                CipherDatabase.OPEN_READONLY,
                null
            ).use { encrypted ->
                encrypted.rawQuery("SELECT value FROM sample").use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals("preserved", cursor.getString(0))
                }
            }
            assertFalse(File(file.path + ".plaintext-backup").exists())
        } finally {
            directory.deleteRecursively()
        }
    }
}
