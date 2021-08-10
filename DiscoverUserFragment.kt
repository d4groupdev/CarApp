package com.example.app.ui.discover.users.fragment

import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.app.R
import com.example.app.data.model.BodyBuilder
import com.example.app.data.model.FeedFilter
import com.example.app.data.model.User
import com.example.app.di.component.FragmentComponent
import com.example.app.ui.base.BaseFragment
import com.example.app.ui.common.SearchFragmentDirections
import com.example.app.ui.discover.adapter.FilterFragmentHost
import com.example.app.ui.explore.ExploreViewModel
import com.example.app.ui.profile.ProfileViewModel
import com.example.app.ui.social.group.adapter.CreateGroupAdminsAdapter
import com.example.app.utils.EndlessNestedScrollViewScrollListener
import kotlinx.android.synthetic.main.fragment_discover_user.*
import javax.inject.Inject

class DiscoverUserFragment : BaseFragment(), FilterFragmentHost,
    SwipeRefreshLayout.OnRefreshListener, CreateGroupAdminsAdapter.FollowActionListener {

    override fun createLayout(): Int  = R.layout.fragment_discover_user

    override fun inject(fragmentComponent: FragmentComponent) = component.inject(this)

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val exploreViewModel by lazy {
        ViewModelProvider(this, viewModelFactory).get(ExploreViewModel::class.java)
    }

    private val profileViewModel by lazy {
        ViewModelProvider(this, viewModelFactory).get(ProfileViewModel::class.java)
    }

    private val scrollListener by lazy {
        object : EndlessNestedScrollViewScrollListener() {
            override fun loadMore(page: Int) {
                loadData(page)
            }
        }
    }

    private var feedFilter: FeedFilter = FeedFilter()
    private var searchKeyword: String? = null

    private val peopleAdapter: CreateGroupAdminsAdapter by lazy { CreateGroupAdminsAdapter(
        CreateGroupAdminsAdapter.SELECTION_FOLLOW,this, type) }

    private val type by lazy { arguments?.getString("type") ?: FeedFilter.FILTER_TYPE_PEOPLE }

    override fun bindData() {
        swipeRefresh.setOnRefreshListener(this)

        exploreViewModel.peopleLiveData.observe(this, {

            if(it.data?.collection?.size == 0){
                pbDiscoverUser.visibility = View.GONE
            }

            scrollListener.prevPageSize = it.data?.collection?.size ?: 0
            peopleAdapter.setItems(it.data?.collection, it.data?.page ?: 1)
            if (it.data?.isEmpty == true) {
                peopleAdapter.errorMessage = it.message
            }
            scrollListener.isLoading = false
        })

        profileViewModel.followUnfollowLiveData.observe(this, {
            peopleAdapter.notifyDataSetChanged()
        })

        recyclerViewUsers.apply {
            adapter = peopleAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
        nestedScrollDiscoverUsers.setOnScrollChangeListener(scrollListener)

        onRefresh()
    }

    override fun fillData() { }

    private fun loadData(page: Int) {
        exploreViewModel.loadPeople(
            BodyBuilder(
                page = page,
                type = type,
                filter = feedFilter,
                searchText = searchKeyword
            )
        )
    }

    override fun filterUpdated(filter: FeedFilter) {
        feedFilter = filter
        scrollListener.currentPage = 1
        loadData(1)
    }

    override fun searchTextUpdated(search: String?) {
        searchKeyword = search
        scrollListener.currentPage = 1
        loadData(1)
    }

    override fun getFilter(): FeedFilter? = feedFilter

    override fun onRefresh() {
        scrollListener.currentPage = 1
        loadData(1)
    }

    override fun onClickFollow(user: User) {
        profileViewModel.follow(user.followState!!, user.id!!)
    }

    override fun onClick(user: User, view: View) {
        if (user.id != session.userId) {
            if (user.isBusiness) {
                val action = SearchFragmentDirections.actionGlobalToProfileBusiness()
                action.user = user
                findNavController().navigate(action)
            } else {
                val action = SearchFragmentDirections.actionGlobalToProfile()
                action.user = user
                findNavController().navigate(action)
            }
        }
    }
}
