package app.axolotl

import app.axolotl.data.PlaintextDatabaseMigrator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PlaintextDatabaseMigratorTest {
    @Test
    fun recognizesPlaintextSqliteHeader() {
        val file = File.createTempFile("axolotl-plain", ".db")
        try {
            file.writeBytes("SQLite format 3\u0000remaining data".toByteArray())
            assertTrue(PlaintextDatabaseMigrator.hasPlaintextHeader(file))
        } finally {
            file.delete()
        }
    }

    @Test
    fun rejectsEncryptedOrTruncatedFiles() {
        val file = File.createTempFile("axolotl-encrypted", ".db")
        try {
            file.writeBytes(byteArrayOf(1, 2, 3, 4))
            assertFalse(PlaintextDatabaseMigrator.hasPlaintextHeader(file))
        } finally {
            file.delete()
        }
    }

}
