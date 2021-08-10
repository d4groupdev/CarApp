package com.example.app.ui.discover.car.fragment

import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.paris.utils.setPaddingTop
import com.example.app.R
import com.example.app.data.model.BodyBuilder
import com.example.app.data.model.FeedFilter
import com.example.app.data.model.Vehicle
import com.example.app.di.component.FragmentComponent
import com.example.app.ui.base.BaseFragment
import com.example.app.ui.base.adapter.OnRecycleItemClick
import com.example.app.ui.discover.adapter.FilterFragmentHost
import com.example.app.ui.discover.car.adapter.DiscoverCarAdapter
import com.example.app.ui.discover.fragment.DiscoverFragmentDirections
import com.example.app.ui.explore.ExploreViewModel
import com.example.app.ui.profile.adapter.GarageCarAdapter
import com.example.app.ui.views.SpannedGridLayoutManager
import com.example.app.utils.EndlessNestedScrollViewScrollListener
import kotlinx.android.synthetic.main.fragment_discover_car.*
import kotlinx.android.synthetic.main.fragment_discover_car.swipeRefresh
import javax.inject.Inject

class DiscoverCarFragment : BaseFragment(), OnRecycleItemClick<Vehicle>, FilterFragmentHost {
    override fun createLayout(): Int = R.layout.fragment_discover_car

    override fun inject(fragmentComponent: FragmentComponent) = component.inject(this)

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var gridLayoutManager: SpannedGridLayoutManager

    private val scrollListener by lazy {
        object : EndlessNestedScrollViewScrollListener() {
            override fun loadMore(page: Int) {
                loadData(page)
            }
        }
    }

    private var feedFilter: FeedFilter = FeedFilter()

    private var searchKeyword: String? = null

    private val exploreViewModel by lazy {
        ViewModelProvider(this, viewModelFactory).get(ExploreViewModel::class.java)
    }

    private val discoverCarAdapter: DiscoverCarAdapter by lazy {
        DiscoverCarAdapter(
            requireContext(), this
        )
    }
    private val favoriteCarAdapter: GarageCarAdapter by lazy { GarageCarAdapter(carClickListener) }

    private val isFavorite: Boolean by lazy {
        arguments?.getBoolean("is_favorite", false) ?: false
    }

    private val isSearch: Boolean? by lazy {
        arguments?.getBoolean("is_search", false)
    }

    override fun bindData() {
        swipeRefresh.setOnRefreshListener { fillData() }

        gridLayoutManager = SpannedGridLayoutManager(SpannedGridLayoutManager.GridSpanLookup {
            when ((it + 1) % 8) {
                1 -> SpannedGridLayoutManager.SpanInfo(4, 4)
                4, 5 -> SpannedGridLayoutManager.SpanInfo(3, 3)
                else -> SpannedGridLayoutManager.SpanInfo(2, 2)
            }
        }, 6, 1f)

        if (isFavorite || isSearch == true) {
            recyclerViewDiscoverCar.layoutManager = LinearLayoutManager(requireContext())

            recyclerViewDiscoverCar.adapter = favoriteCarAdapter
            recyclerViewDiscoverCar.setPaddingTop(resources.getDimensionPixelOffset(R.dimen.form_padding))
        } else {
            discoverCarAdapter.type = DiscoverCarAdapter.TYPE_GRID_VIEW
            recyclerViewDiscoverCar.layoutManager = gridLayoutManager

            recyclerViewDiscoverCar.adapter = discoverCarAdapter
        }
        nestedScrollDiscoverCar.setOnScrollChangeListener(scrollListener)

        exploreViewModel.carsLiveData.observeOwner(this, {
        if (!isFavorite && isSearch == false) {
            if (it.data?.page ?: 1 == 1 && it.data?.collection?.size == 1)
                recyclerViewDiscoverCar.layoutManager = GridLayoutManager(context, 2)
            else recyclerViewDiscoverCar.layoutManager = gridLayoutManager
        }

        scrollListener.prevPageSize = it.data?.collection?.size ?: 0
        (if (isFavorite || isSearch == true)
            favoriteCarAdapter
        else
            discoverCarAdapter)
            .setItems(it.data?.collection, it.data?.page ?: 1)

            if (!isFavorite) {
                if(it.data?.collection?.size == 0){
                    pbDiscoverCar.visibility = View.GONE
                }
            }else {
                pbDiscoverCar.visibility = View.GONE
            }

            if (it.data?.isEmpty == true) {
            (if (isFavorite || isSearch == true)
                favoriteCarAdapter
            else
                discoverCarAdapter).errorMessage = it.message
        }
        scrollListener.isLoading = false
        })
}

    override fun fillData() {
        loadData(1)
    }

    override fun onResume() {
        super.onResume()
        loadData(1)
    }

    private fun loadData(page: Int) {
        exploreViewModel.loadCars(
            BodyBuilder(
                page = page,
                type = FeedFilter.FILTER_TYPE_CAR,
                filter = feedFilter,
                searchText = searchKeyword,
                isFavorite = isFavorite
            )
        )
    }

    override fun onClick(t: Vehicle, view: View) {
        navigateVehicle(t)
    }

    private fun navigateVehicle(vehicle: Vehicle) {
        val action =
            DiscoverFragmentDirections.actionGlobalVehicleDetails()
        action.vehicle = vehicle
        findNavController().navigate(action)
    }

    private val carClickListener = object : GarageCarAdapter.ActivityActionListener {
        override fun onRemove(vehicle: Vehicle?, position: Int) {

        }

        override fun onClick(t: Vehicle, view: View) {
            navigateVehicle(t)
        }
    }

    override fun filterUpdated(filter: FeedFilter) {
        feedFilter = filter
        scrollListener.currentPage = 1
        loadData(1)
    }

    override fun getFilter(): FeedFilter? = feedFilter

    override fun searchTextUpdated(search: String?) {
        searchKeyword = search
        scrollListener.currentPage = 1
        loadData(1)
    }
}