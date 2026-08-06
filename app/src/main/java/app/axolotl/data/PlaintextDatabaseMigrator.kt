package app.axolotl.data

import net.zetetic.database.sqlcipher.SQLiteDatabase
import java.io.File
import java.io.FileInputStream

internal object PlaintextDatabaseMigrator {
    private val SQLITE_HEADER = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)

    fun migrateIfNeeded(databaseFile: File, passphrase: ByteArray) {
        System.loadLibrary("sqlcipher")
        recoverInterruptedReplacement(databaseFile, passphrase)
        if (!databaseFile.exists() || !hasPlaintextHeader(databaseFile)) return

        val encryptedFile = File(databaseFile.path + ".encrypted-new")
        val backupFile = File(databaseFile.path + ".plaintext-backup")
        encryptedFile.delete()
        exportEncrypted(databaseFile, encryptedFile, passphrase)
        verifyEncrypted(encryptedFile, passphrase)

        check(databaseFile.renameTo(backupFile)) { "Could not preserve plaintext database during encryption" }
        try {
            check(encryptedFile.renameTo(databaseFile)) { "Could not install encrypted database" }
            verifyEncrypted(databaseFile, passphrase)
            deleteSidecars(databaseFile)
            check(backupFile.delete()) { "Encrypted database installed but plaintext backup could not be deleted" }
        } catch (error: Throwable) {
            databaseFile.delete()
            backupFile.renameTo(databaseFile)
            throw error
        } finally {
            encryptedFile.delete()
        }
    }

    internal fun hasPlaintextHeader(file: File): Boolean {
        if (!file.isFile || file.length() < SQLITE_HEADER.size) return false
        return FileInputStream(file).use { input ->
            val header = ByteArray(SQLITE_HEADER.size)
            input.read(header) == header.size && header.contentEquals(SQLITE_HEADER)
        }
    }

    private fun exportEncrypted(source: File, destination: File, passphrase: ByteArray) {
        val database = SQLiteDatabase.openDatabase(
            source.path,
            ByteArray(0),
            null,
            SQLiteDatabase.OPEN_READWRITE,
            null
        )
        try {
            database.rawExecSQL("PRAGMA wal_checkpoint(FULL)")
            database.rawExecSQL("ATTACH DATABASE ? AS encrypted KEY ?", destination.path, passphrase)
            database.rawQuery("SELECT sqlcipher_export('encrypted')").use { it.moveToFirst() }
            database.rawExecSQL("DETACH DATABASE encrypted")
        } finally {
            database.close()
        }
    }

    private fun verifyEncrypted(file: File, passphrase: ByteArray) {
        check(!hasPlaintextHeader(file)) { "SQLCipher export still has a plaintext SQLite header" }
        val database = SQLiteDatabase.openDatabase(
            file.path,
            passphrase,
            null,
            SQLiteDatabase.OPEN_READONLY,
            null
        )
        try {
            check(database.isDatabaseIntegrityOk) { "Encrypted database integrity check failed" }
            database.rawQuery("SELECT count(*) FROM sqlite_master").use { cursor ->
                check(cursor.moveToFirst()) { "Encrypted database schema is unreadable" }
            }
        } finally {
            database.close()
        }
    }

    private fun recoverInterruptedReplacement(databaseFile: File, passphrase: ByteArray) {
        val backupFile = File(databaseFile.path + ".plaintext-backup")
        if (!backupFile.exists()) return
        if (databaseFile.exists()) {
            runCatching { verifyEncrypted(databaseFile, passphrase) }
                .onSuccess {
                    backupFile.delete()
                    return
                }
            databaseFile.delete()
        }
        check(backupFile.renameTo(databaseFile)) { "Could not restore interrupted database migration" }
    }

    private fun deleteSidecars(databaseFile: File) {
        File(databaseFile.path + "-wal").delete()
        File(databaseFile.path + "-shm").delete()
        File(databaseFile.path + "-journal").delete()
    }
}
