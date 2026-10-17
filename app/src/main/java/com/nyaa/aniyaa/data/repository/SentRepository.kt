package com.nyaa.aniyaa.data.repository

import com.nyaa.aniyaa.data.db.AppDatabase
import com.nyaa.aniyaa.data.db.SentListingEntity
import kotlinx.coroutines.flow.Flow

class SentRepository(private val database: AppDatabase) {
    private val dao = database.sentListingDao()

    fun observe(): Flow<List<String>> = dao.observe()

    suspend fun add(identity: String) {
        if (identity.isBlank()) return
        dao.upsert(SentListingEntity(identity, System.currentTimeMillis()))
        dao.trim(MAX_SENT)
    }

    suspend fun remove(identity: String) {
        dao.delete(identity)
    }

    suspend fun clear() {
        dao.deleteAll()
    }

    companion object {
        const val MAX_SENT = 400
    }
}
