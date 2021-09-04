package com.bbgo.module_project.viewmodel

import androidx.lifecycle.*
import com.bbgo.common_base.BaseApplication
import com.bbgo.common_base.ext.HTTP_REQUEST_ERROR
import com.bbgo.common_base.ext.Resource
import com.bbgo.common_base.ext.logE
import com.bbgo.common_base.util.NetWorkUtil
import com.bbgo.module_project.bean.ArticleDetail
import com.bbgo.module_project.bean.ProjectBean
import com.bbgo.module_project.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 *  author: wangyb
 *  date: 3/29/21 9:31 PM
 *  description: todo
 */
@HiltViewModel
class ProjectViewModel @Inject constructor(private val repository: ProjectRepository) : ViewModel() {

    val projectTreeLiveData by lazy { MutableLiveData<Resource<List<ProjectBean>>>() }
    val articlesLiveData by lazy { MutableLiveData<Resource<MutableList<ArticleDetail>>>() }
    private val articleList = mutableListOf<ArticleDetail>()

    fun getProjectTree() {
        viewModelScope.launch {
            /**
             * 1.必须要有异常处理
             * 2.必须要有collect，否则map里面的代码不执行
             */
            if (NetWorkUtil.isNetworkConnected(BaseApplication.getContext())) {
                repository.getProjectTree()
                    .map {
                        if (it.errorCode == HTTP_REQUEST_ERROR) {
                            Resource.DataError(it.errorCode, it.errorMsg)
                        } else {
                            Resource.Success(it.data)
                        }
                    }
                    .catch {
                    }
                    .flowOn(Dispatchers.IO)
                    .collectLatest {
                        projectTreeLiveData.value = it

                        insertProjectTree()
                    }
                return@launch
            }
            /**
             * 无网络从DB中查询数据
             */
            repository.getProjectTreeFromDB()
                .catch {

                }
                .flowOn(Dispatchers.IO)
                .collectLatest {
                    projectTreeLiveData.value = Resource.Success(it)
                }
        }
    }

    /**
     * 将从网络中获取的数据存入DB
     */
    private fun insertProjectTree() {
        viewModelScope.launch {
            projectTreeLiveData.value?.data?.let {
                repository.insertProjectTree(it)
            }
        }
    }

    fun getProjectList(id: Int, page: Int) {
        viewModelScope.launch {
            /**
             * 1.必须要有异常处理
             * 2.必须要有collect，否则map里面的代码不执行
             */
            if (NetWorkUtil.isNetworkConnected(BaseApplication.getContext())) {
                repository.getProjectList(id, page)
                    .map {
                        if (it.errorCode == HTTP_REQUEST_ERROR) {
                            Resource.DataError(it.errorCode, it.errorMsg)
                        } else {
                            Resource.Success(it.data.datas)
                        }
                    }
                    .catch {
                        logE(TAG, it.message, it)
                    }
                    .collect {
                        articlesLiveData.value = it

                        insertProjectArticle()
                    }
                return@launch
            }


            repository.getArticleDetailWithTagFromDB()
                .map {
                    articleList.clear()
                    it.forEach { articleDetailWithTag ->
                        articleDetailWithTag.articleDetail.tags = articleDetailWithTag.tags
                        articleList.add(articleDetailWithTag.articleDetail)
                    }
                    Resource.Success(articleList)
                }
                .catch {  }
                .flowOn(Dispatchers.IO)
                .collectLatest {
                    articlesLiveData.value = it
                }
        }
    }

    private fun insertProjectArticle() {
        articlesLiveData.value?.data?.let {
            viewModelScope.launch {
                repository.insertProjectArticles(it)
            }
        }
    }

    private val TAG = "ProjectViewModel"
}