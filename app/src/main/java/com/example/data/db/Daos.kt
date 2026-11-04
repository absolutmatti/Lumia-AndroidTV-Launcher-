package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.model.FavoriteApp
import com.example.data.model.HiddenApp
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteAppDao {
    @Query("SELECT * FROM favorite_app ORDER BY position ASC")
    fun getAllFavorites(): Flow<List<FavoriteApp>>

    @Query("SELECT * FROM favorite_app ORDER BY position ASC")
    suspend fun getAllFavoritesList(): List<FavoriteApp>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: FavoriteApp)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(favorites: List<FavoriteApp>)

    @Query("DELETE FROM favorite_app WHERE packageName = :packageName")
    suspend fun deleteByPackage(packageName: String)

    @Query("DELETE FROM favorite_app")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM favorite_app")
    suspend fun getCount(): Int

    @Transaction
    suspend fun renumberFavorites(favorites: List<FavoriteApp>) {
        clearAll()
        val reindexed = favorites.mapIndexed { index, item ->
            FavoriteApp(item.packageName, index)
        }
        insertAll(reindexed)
    }
}

@Dao
interface HiddenAppDao {
    @Query("SELECT * FROM hidden_app")
    fun getAllHidden(): Flow<List<HiddenApp>>

    @Query("SELECT * FROM hidden_app")
    suspend fun getAllHiddenList(): List<HiddenApp>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(hidden: HiddenApp)

    @Query("DELETE FROM hidden_app WHERE packageName = :packageName")
    suspend fun deleteByPackage(packageName: String)

    @Query("DELETE FROM hidden_app")
    suspend fun clearAll()
}
