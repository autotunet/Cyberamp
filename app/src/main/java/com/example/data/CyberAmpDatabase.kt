package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "eq_presets")
data class EqPresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val gainsCsv: String,       // 10 comma-separated floats
    val qFactorsCsv: String,    // 10 comma-separated floats
    val preampDb: Float,
    val tubeWarmth: Float,
    val subBassBoost: Float,
    val stereoWidth: Float,
    val isBuiltIn: Boolean = false
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val moodDescription: String,
    val trackIdsCsv: String,
    val aiReasoning: String,
    val recommendedPresetName: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorite_tracks")
data class FavoriteTrackEntity(
    @PrimaryKey val trackId: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Dao
interface CyberAmpDao {
    @Query("SELECT * FROM eq_presets ORDER BY isBuiltIn DESC, id ASC")
    fun getAllEqPresets(): Flow<List<EqPresetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEqPreset(preset: EqPresetEntity)

    @Query("DELETE FROM eq_presets WHERE id = :id AND isBuiltIn = 0")
    suspend fun deleteCustomEqPreset(id: Int)

    @Query("SELECT COUNT(*) FROM eq_presets")
    suspend fun getEqPresetCount(): Int

    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: Int)

    @Query("SELECT COUNT(*) FROM playlists")
    suspend fun getPlaylistCount(): Int

    @Query("SELECT * FROM favorite_tracks")
    fun getAllFavorites(): Flow<List<FavoriteTrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(fav: FavoriteTrackEntity)

    @Query("DELETE FROM favorite_tracks WHERE trackId = :trackId")
    suspend fun removeFavorite(trackId: String)
}

@Database(
    entities = [EqPresetEntity::class, PlaylistEntity::class, FavoriteTrackEntity::class],
    version = 1,
    exportSchema = false
)
abstract class CyberAmpDatabase : RoomDatabase() {
    abstract fun dao(): CyberAmpDao

    companion object {
        @Volatile
        private var INSTANCE: CyberAmpDatabase? = null

        fun getInstance(context: Context): CyberAmpDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CyberAmpDatabase::class.java,
                    "cyberamp_pro.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
