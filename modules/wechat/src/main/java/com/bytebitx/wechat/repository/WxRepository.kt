package com.bytebitx.wechat.repository

import com.bytebitx.base.bean.HttpResult
import com.bytebitx.wechat.bean.ArticleData
import com.bytebitx.wechat.bean.WXArticle
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 *  author: wangyb
 *  date: 3/29/21 9:32 PM
 *  description: todo
 */
@ActivityRetainedScoped
class WxRepository @Inject constructor(private val remoteRepository: WxRemoteRepository, private val localRepository: WxLocalRepository) {

    fun getWXChapters() : Flow<HttpResult<List<WXArticle>>> {
        return remoteRepository.getWXChapters()
    }

    fun getWXArticles(id: Int, page: Int) : Flow<HttpResult<ArticleData>> {
        return remoteRepository.getWXArticles(id, page)
    }

    fun getKnowledgeList(id: Int, page: Int) : Flow<HttpResult<ArticleData>> {
        return remoteRepository.getKnowledgeList(id, page)
    }
}