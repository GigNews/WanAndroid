package com.bytebitx.home.ui

import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import cn.bingoogolapple.bgabanner.BGABanner
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.bytebitx.base.base.BaseFragment
import com.bytebitx.base.constants.Constants
import com.bytebitx.base.constants.RouterPath
import com.bytebitx.base.databinding.LayoutLoadingBinding
import com.bytebitx.base.event.MessageEvent
import com.bytebitx.base.ext.Resource
import com.bytebitx.base.ext.collectWithLifeCycle
import com.bytebitx.base.ext.showToast
import com.bytebitx.base.util.ImageLoader
import com.bytebitx.base.util.log.Logs
import com.bytebitx.base.widget.SpaceItemDecoration
import com.bytebitx.home.R
import com.bytebitx.home.bean.ArticleDetail
import com.bytebitx.home.bean.Banner
import com.bytebitx.home.databinding.FragmentHomeBinding
import com.bytebitx.home.databinding.ItemHomeBannerBinding
import com.bytebitx.home.viewmodel.HomeViewModel
import com.bytebitx.service.collect.CollectService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject


/**
 *  author: wangyb
 *  date: 2021/5/20 3:00 下午
 *  description: todo
 */
@Route(path = RouterPath.Home.PAGE_HOME)
@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding>() {

    companion object {

        fun getInstance(): HomeFragment {
            return HomeFragment()
        }
    }

    private lateinit var bannerBinding: ItemHomeBannerBinding
    private lateinit var loadingBinding: LayoutLoadingBinding

    @Inject
    lateinit var homeViewModel: HomeViewModel
    @Autowired(name = RouterPath.Collect.SERVICE_COLLECT)
    lateinit var collectService: CollectService

    /**
     * RecyclerView Divider
     */
    private val recyclerViewItemDecoration by lazy {
        activity?.let {
            SpaceItemDecoration(it)
        }
    }

    /**
     * LinearLayoutManager
     */
    private val linearLayoutManager: LinearLayoutManager by lazy {
        LinearLayoutManager(activity)
    }

    /**
     * datas
     */
    private val articleList = mutableListOf<ArticleDetail>()

    /**
     * Home Adapter
     */
    private val homeAdapter: HomeAdapter by lazy {
        HomeAdapter(articleList)
    }

    /**
     * is Refresh
     */
    private var isRefresh = false

    /**
     * RefreshListener
     */
    private val onRefreshListener = SwipeRefreshLayout.OnRefreshListener {
        isRefresh = true
        homeViewModel.getArticles(0)
    }

    /**
     * Banner Adapter
     */
    private val bannerAdapter: BGABanner.Adapter<ImageView, String> by lazy {
        BGABanner.Adapter<ImageView, String> { bgaBanner, imageView, feedImageUrl, position ->
            activity?.let { ImageLoader.load(it, feedImageUrl, imageView) }
        }
    }

    override fun initView() {
        bannerBinding = ItemHomeBannerBinding.inflate(layoutInflater)
        loadingBinding = LayoutLoadingBinding.bind(binding.root)

        ARouter.getInstance().inject(this)
        initBus()
        binding.swipeRefreshLayout.run {
            setOnRefreshListener(onRefreshListener)
        }
        binding.recyclerView.run {
            layoutManager = linearLayoutManager
            adapter = homeAdapter
            itemAnimator = DefaultItemAnimator()
            recyclerViewItemDecoration?.let { addItemDecoration(it) }
        }

        homeAdapter.run {
            addHeaderView(bannerBinding.root)
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
                            Constants.FragmentIndex.HOME_INDEX,
                            position,
                            articleList[position].id
                        )
                        return@setOnItemChildClickListener
                    }
                    collectService.collect(
                        Constants.FragmentIndex.HOME_INDEX,
                        position,
                        articleList[position].id
                    )
                }
            }
        }
    }

    /**
     * 初始化事件总线，和eventbus效果相同
     */
    private fun initBus() {
//        LiveDataBus.get().with(BusKey.COLLECT, MessageEvent::class.java).observe(viewLifecycleOwner) {
//            if (it.indexPage == Constants.FragmentIndex.HOME_INDEX) {
//                handleCollect(it)
//            }
//        }
//        LiveDataBus.get().with(BusKey.SCROLL_TOP, ScrollEvent::class.java).observe(viewLifecycleOwner) {
//            if (it.index == 0) {
//                scrollToTop()
//            }
//        }
    }

    override fun lazyLoad() {
        homeViewModel.getArticles(0)
        homeViewModel.getBanner()
    }

    override fun initObserver() {
        collectWithLifeCycle(this) {
            /**
             * flow默认情况下是不管页面处于哪个生命周期都会订阅数据，不会像livedata一样，
             * 在生命周期处于DESTROYED时，移除观察者。因此需要在start生命周期启动协程达到
             * 和livedata一样的效果
             *
             * 收集多个stateflow，需要launch多次
             */
            launch {
                homeViewModel.articleUiState.collectLatest {
                    handleInfo(it)
                }
            }
            launch {
                homeViewModel.bannerUiState.collectLatest {
                    handleBanner(it)
                }
            }
        }
    }


    private fun handleBanner(status: Resource<List<Banner>>) {
        when(status) {
            is Resource.Loading -> {

            }
            is Resource.Error -> {
                Logs.e(status.exception, "")
            }
            is Resource.Success -> {
                val bannerFeedList = ArrayList<String>()
                val bannerTitleList = ArrayList<String>()
                status.data.forEach { banner ->
                    bannerFeedList.add(banner.imagePath)
                    bannerTitleList.add(banner.title)
                }
                bannerBinding.banner.setDelegate { banner, imageView, model, position ->

                }
                bannerBinding.banner.run {
                    setAutoPlayAble(bannerFeedList.size > 1)
                    setData(bannerFeedList, bannerTitleList)
                    setAdapter(bannerAdapter)
                }
            }
        }
    }

    private fun handleInfo(articles: Resource<List<ArticleDetail>>) {
        when(articles) {
            is Resource.Loading -> {
                loadingBinding.progressBar.visibility = View.VISIBLE
            }
            is Resource.Error -> {
                loadingBinding.progressBar.visibility = View.GONE
                binding.swipeRefreshLayout.isRefreshing = false
            }
            is Resource.Success -> {
                loadingBinding.progressBar.visibility = View.GONE
                binding.swipeRefreshLayout.isRefreshing = false
                if (isRefresh) {
                    articleList.clear()
                    articleList.addAll(articles.data)
                    homeAdapter.setList(articleList)
                } else {
                    articleList.addAll(articles.data)
                    homeAdapter.addData(articleList)
                }
            }
        }
    }

    private fun handleCollect(event: MessageEvent) {
        when (event.type) {
            Constants.CollectType.UNKNOWN -> {
                ARouter.getInstance().build(RouterPath.LoginRegister.PAGE_LOGIN).navigation()
            }
            else -> {
                if (event.type == Constants.CollectType.COLLECT) {
                    showToast(getString(R.string.collect_success))
                    articleList[event.position].collect = true
                    homeAdapter.setList(articleList)
                    return
                }
                articleList[event.position].collect = false
                homeAdapter.setList(articleList)
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