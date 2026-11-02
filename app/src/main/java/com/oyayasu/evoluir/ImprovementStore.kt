package com.oyayasu.evoluir

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/** All data stays on the device; no account, analytics, or network dependency. */
class ImprovementStore(context: Context) : SQLiteOpenHelper(context.applicationContext, "evoluir.db", null, 1) {
    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE improvements (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                description TEXT NOT NULL DEFAULT '',
                symbol TEXT NOT NULL DEFAULT 'sprout',
                created_at INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE daily_marks (
                improvement_id INTEGER NOT NULL,
                date TEXT NOT NULL,
                outcome INTEGER NOT NULL CHECK(outcome IN (0,1)),
                note TEXT NOT NULL DEFAULT '',
                PRIMARY KEY (improvement_id, date),
                FOREIGN KEY (improvement_id) REFERENCES improvements(id) ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX marks_by_date ON daily_marks(date)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Future migrations must preserve existing user entries.
    }

    fun list(): List<ImprovementSummary> {
        val db = readableDatabase
        val summaries = mutableListOf<ImprovementSummary>()
        db.rawQuery("SELECT id, name, description, created_at, symbol FROM improvements ORDER BY created_at DESC, id DESC", null).use { c ->
            while (c.moveToNext()) {
                val improvement = Improvement(c.getLong(0), c.getString(1), c.getString(2), c.getLong(3), c.getString(4))
                val marks = mutableListOf<DailyMark>()
                db.rawQuery(
                    "SELECT date, outcome, note FROM daily_marks WHERE improvement_id = ? ORDER BY date DESC",
                    arrayOf(improvement.id.toString())
                ).use { m ->
                    while (m.moveToNext()) marks += DailyMark(m.getString(0), m.getInt(1), m.getString(2))
                }
                summaries += ImprovementSummary(improvement, marks)
            }
        }
        return summaries
    }

    fun save(id: Long?, name: String, description: String, symbol: String) {
        val cleanName = name.trim()
        require(cleanName.isNotEmpty()) { "Informe um título" }
        val values = ContentValues().apply {
            put("name", cleanName)
            put("description", description.trim())
            put("symbol", symbol.take(32).ifBlank { "sprout" })
        }
        if (id == null) {
            values.put("created_at", System.currentTimeMillis())
            writableDatabase.insertOrThrow("improvements", null, values)
        } else {
            writableDatabase.update("improvements", values, "id = ?", arrayOf(id.toString()))
        }
    }

    fun delete(id: Long) {
        writableDatabase.delete("improvements", "id = ?", arrayOf(id.toString()))
    }

    /** Passing null preserves an existing note when changing today's mark. */
    fun mark(id: Long, day: String, outcome: Int, note: String? = null) {
        require(outcome in 0..1)
        val db = writableDatabase
        val previousNote = if (note == null) {
            db.rawQuery("SELECT note FROM daily_marks WHERE improvement_id = ? AND date = ?", arrayOf(id.toString(), day)).use { c ->
                if (c.moveToFirst()) c.getString(0) else ""
            }
        } else note
        db.insertWithOnConflict("daily_marks", null, ContentValues().apply {
            put("improvement_id", id)
            put("date", day)
            put("outcome", outcome)
            put("note", previousNote)
        }, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun clear(id: Long, day: String) {
        writableDatabase.delete("daily_marks", "improvement_id = ? AND date = ?", arrayOf(id.toString(), day))
    }
}
