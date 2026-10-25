package com.byga.smartoverlay.agent

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName="agent_tasks")
data class AgentTaskRow(@PrimaryKey val id: String, val goal: String, val state: String, val transcript: String, val updatedAt: Long)
@Entity(tableName="agent_memory")
data class MemoryEntry(@PrimaryKey val id: String, val text: String, val consentAt: Long)
@Entity(tableName="agent_skills",primaryKeys=["id","version"])
data class SkillRow(val id: String, val version: Int, val name: String, val planJson: String, val status: String)
@Entity(tableName="agent_compatibility")
data class CompatibilityRow(@PrimaryKey val id: String, val os: String, val packageName: String, val appVersion: String, val operation: String, val result: String, val testedAt: Long)
@Dao interface AgentRecords {
    @Query("SELECT * FROM agent_tasks ORDER BY updatedAt DESC") fun tasks(): Flow<List<AgentTaskRow>>
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun saveTask(task: AgentTaskRow)
    @Query("DELETE FROM agent_tasks") suspend fun clearTasks()
    @Query("SELECT * FROM agent_memory ORDER BY consentAt DESC") fun memories(): Flow<List<MemoryEntry>>
    @Query("SELECT * FROM agent_memory WHERE instr(lower(text), lower(:query)) > 0 ORDER BY consentAt DESC LIMIT 50") suspend fun searchMemory(query: String): List<MemoryEntry>
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun saveMemory(memory: MemoryEntry)
    @Query("DELETE FROM agent_memory WHERE id=:id") suspend fun forget(id: String)
    @Query("DELETE FROM agent_memory") suspend fun forgetAll()
    @Query("SELECT * FROM agent_skills ORDER BY name, version DESC") fun skills(): Flow<List<SkillRow>>
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun saveSkill(skill: SkillRow)
    @Query("DELETE FROM agent_skills WHERE id=:id AND version=:version") suspend fun deleteSkill(id: String, version: Int)
    @Query("SELECT * FROM agent_compatibility ORDER BY testedAt DESC") fun compatibility(): Flow<List<CompatibilityRow>>
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun recordCompatibility(row: CompatibilityRow)
}
val AGENT_MIGRATION_1_2 = object : Migration(1,2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS agent_tasks (id TEXT NOT NULL PRIMARY KEY, goal TEXT NOT NULL, state TEXT NOT NULL, transcript TEXT NOT NULL, updatedAt INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS agent_memory (id TEXT NOT NULL PRIMARY KEY, text TEXT NOT NULL, consentAt INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS agent_skills (id TEXT NOT NULL, version INTEGER NOT NULL, name TEXT NOT NULL, planJson TEXT NOT NULL, status TEXT NOT NULL, PRIMARY KEY(id,version))")
        db.execSQL("CREATE TABLE IF NOT EXISTS agent_compatibility (id TEXT NOT NULL PRIMARY KEY, os TEXT NOT NULL, packageName TEXT NOT NULL, appVersion TEXT NOT NULL, operation TEXT NOT NULL, result TEXT NOT NULL, testedAt INTEGER NOT NULL)")
    }
}
enum class SkillStatus { DRAFT, TESTING, REVIEW, APPROVED, ENABLED, DISABLED, DEPRECATED }
object SkillLifecycle {
    fun transition(from: SkillStatus, to: SkillStatus, testPassed: Boolean): Boolean = when(from) {
        SkillStatus.DRAFT -> to==SkillStatus.TESTING
        SkillStatus.TESTING -> to==SkillStatus.REVIEW && testPassed
        SkillStatus.REVIEW -> to==SkillStatus.APPROVED && testPassed
        SkillStatus.APPROVED -> to==SkillStatus.ENABLED && testPassed
        SkillStatus.ENABLED -> to in setOf(SkillStatus.DISABLED,SkillStatus.DEPRECATED)
        SkillStatus.DISABLED -> to==SkillStatus.TESTING
        SkillStatus.DEPRECATED -> false
    }
}
object AgentSessions {
    @Volatile var active: AgentController?=null
    fun stopAll() { active?.stop(); active=null }
}
