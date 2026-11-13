
package com.eniola.smartstudyplanner.database

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.OnConflictStrategy

/** Defines SQLite operations for study tasks.*/
@Dao
interface TaskDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveTask(task: TaskEntity)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("SELECT * FROM tasks WHERE userId = :userId")
    fun getTasks(userId: String): LiveData<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :taskId AND userId = :userId LIMIT 1")
    suspend fun getTaskById(
        taskId: String,
        userId: String
    ): TaskEntity?

    @Query("DELETE FROM tasks WHERE id = :taskId AND userId = :userId")
    suspend fun deleteTaskById(
        taskId: String,
        userId: String
    )

    // Import existing Firebase tasks without overwriting local records.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun importTasks(tasks: List<TaskEntity>): List<Long>

    // Count all tasks stored locally for a particular student.
    @Query("SELECT COUNT(*) FROM tasks WHERE userId = :userId")
    suspend fun countTasks(userId: String): Int
}
