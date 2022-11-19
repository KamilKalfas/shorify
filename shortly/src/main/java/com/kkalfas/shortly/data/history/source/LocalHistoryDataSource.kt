package com.kkalfas.shortly.data.history.source

import com.kkalfas.shortly.data.history.model.LinkEntryModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class LocalHistoryDataSource : HistoryDataSource {

    override suspend fun shortenUrl(url: String) {
         // noop for Local
    }

    override fun getLinkHistory(): Flow<List<LinkEntryModel>> = flow {

    }
}
