package com.bytebitx.project.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.launcher.ARouter
import com.bytebitx.base.base.BaseFragment
import com.bytebitx.base.constants.Constants
import com.bytebitx.base.constants.RouterPath
import com.bytebitx.base.event.MessageEvent
import com.bytebitx.base.ext.Resource
import com.bytebitx.base.ext.observe
import com.bytebitx.base.ext.showToast
import com.bytebitx.base.widget.SpaceItemDecoration
import com.bytebitx.project.R
import com.bytebitx.project.bean.ArticleDetail
import com.bytebitx.project.databinding.FragmentProjectListBinding
import com.bytebitx.project.viewmodel.ProjectViewModel
import com.bytebitx.service.collect.CollectService
import dagger.hilt.android.AndroidEntryPoint

/**
 * Created by wangyb
 */
@AndroidEntryPoint
class ProjectListFragment : BaseFragment<FragmentProjectListBinding>() {

    companion object {
        fun getInstance(cid: Int): ProjectListFragment {
            val fragment = ProjectListFragment()
            val args = Bundle()
            args.putInt(Constants.CONTENT_CID_KEY, cid)
            fragment.arguments = args
            return fragment
        }
    }

    @Autowired
    lateinit var collectService: CollectService

    private val projectViewModel by viewModels<ProjectViewModel>()

    /**
     * cid
     */
    private var cid: Int = 0

    /**
     * 是否是下拉刷新
     */
    private var isRefresh = true

    /**
     * datas
     */
    private val articleList = mutableListOf<ArticleDetail>()

    /**
     * RecyclerView Divider
     */
    private val recyclerViewItemDecoration by lazy {
        activity?.let {
            SpaceItemDecoration(it)
        }
    }

    private val handler = Handler(Looper.getMainLooper())

    /**
     * LinearLayoutManager
     */
    private val linearLayoutManager: LinearLayoutManager by lazy {
        LinearLayoutManager(activity)
    }

    /**
     * Adapter
     */
    private val mAdapter: ArticleListAdapter by lazy {
        ArticleListAdapter(articleList)
    }

    /**
     * RefreshListener
     */
    private val onRefreshListener = SwipeRefreshLayout.OnRefreshListener {
        binding.swipeRefreshLayout.isRefreshing = false
        isRefresh = true
    }

    override fun initView() {
        ARouter.getInstance().inject(this)

        cid = arguments?.getInt(Constants.CONTENT_CID_KEY) ?: 0
        binding.swipeRefreshLayout.setOnRefreshListener(onRefreshListener)
        binding.recyclerView.run {
            layoutManager = linearLayoutManager
            adapter = mAdapter
            itemAnimator = DefaultItemAnimator()
            recyclerViewItemDecoration?.let { addItemDecoration(it) }
        }

        mAdapter.setOnItemClickListener { adapter, view, position ->
            val article = articleList[position]
            ARouter.getInstance().build(RouterPath.Content.PAGE_CONTENT)
                .withString(Constants.POSITION, position.toString())
                .withString(Constants.CONTENT_ID_KEY, article.id.toString())
                .withString(Constants.CONTENT_TITLE_KEY, article.title)
                .withString(Constants.CONTENT_URL_KEY, article.link)
                .navigation()
        }

        mAdapter.run {
            setOnItemClickListener { adapter, view, position ->
                val article = articleList[position]
                ARouter.getInstance().build(RouterPath.Content.PAGE_CONTENT)
                    .withString(Constants.POSITION, position.toString())
                    .withString(Constants.CONTENT_ID_KEY, article.id.toString())
                    .withString(Constants.CONTENT_TITLE_KEY, article.title)
                    .withString(Constants.CONTENT_URL_KEY, article.link)
                    .withString(Constants.COLLECT, article.collect.toString())
                    .navigation()
            }
            addChildClickViewIds(R.id.iv_like)
            setOnItemChildClickListener { adapter, view, position ->
                if (view.id == R.id.iv_like) {
                    val article = articleList[position]
                    if (article.collect) {
                        collectService.unCollect(
                            Constants.FragmentIndex.PROJECT_INDEX,
                            position,
                            articleList[position].id
                        )
                        return@setOnItemChildClickListener
                    }
                    collectService.collect(
                        Constants.FragmentIndex.PROJECT_INDEX,
                        position,
                        articleList[position].id
                    )
                }
            }
        }

        initBus()
    }

    /**
     * 初始化事件总线，和eventbus效果相同
     */
    private fun initBus() {
//        LiveDataBus.get().with(BusKey.COLLECT, MessageEvent::class.java).observe(this) {
//            if (it.indexPage == Constants.FragmentIndex.PROJECT_INDEX) {
//                handleCollect(it)
//            }
//        }
//        LiveDataBus.get().with(BusKey.SCROLL_TOP, ScrollEvent::class.java).observe(this) {
//            if (it.index == 4) {
//                scrollToTop()
//            }
//        }
    }

    override fun lazyLoad() {
        projectViewModel.getProjectList(cid, 0)
    }

    override fun initObserver() {
        observe(projectViewModel.articlesLiveData, ::handleInfo)
    }

    private fun handleInfo(status: Resource<MutableList<ArticleDetail>>) {
        if (status !is Resource.Success) return
        articleList.clear()
        articleList.addAll(status.data)
        mAdapter.run {
            if (isRefresh) {
                setList(articleList)
            } else {
                addData(articleList)
            }
        }
    }

    private fun handleCollect(event: MessageEvent) {
        when (event.type) {
            Constants.CollectType.UNKNOWN -> {
                ARouter.getInstance().build(RouterPath.LoginRegister.PAGE_LOGIN).navigation()
            }
            else -> {
                if (articleList.isEmpty()) {
                    return
                }
                if (event.type == Constants.CollectType.COLLECT) {
                    showToast(getString(R.string.collect_success))
                    articleList[event.position].collect = true
                    mAdapter.setList(articleList)
                    return
                }
                articleList[event.position].collect = false
                mAdapter.setList(articleList)
                showToast(getString(R.string.cancel_collect_success))
            }
        }
    }

    private fun scrollToTop() {
        binding.recyclerView.run {
            if (linearLayoutManager.findFirstVisibleItemPosition() > 20) {
                scrollToPosition(0)
            } else {
                smoothScrollToPosition(0)
            }
        }
    }
}