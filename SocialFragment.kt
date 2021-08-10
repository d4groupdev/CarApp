package com.example.app.ui.social.fragment


import android.app.Activity
import android.content.Intent
import android.view.View
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.viewpager.widget.ViewPager
import com.example.app.R
import com.example.app.core.Common
import com.example.app.data.model.FeedFilter
import com.example.app.di.component.FragmentComponent
import com.example.app.ui.activity.IsolatedActivity
import com.example.app.ui.base.BaseFragment
import com.example.app.ui.common.SearchFragment
import com.example.app.ui.discover.adapter.FilterFragmentHost
import com.example.app.ui.profile.adapter.CommonFragmentPagerAdapter
import com.example.app.ui.social.event.fragment.EventsFragment
import com.example.app.ui.social.event.fragment.FilterEventsFragment
import com.example.app.ui.social.group.fragment.FilterGroupsFragment
import com.example.app.ui.social.group.fragment.GroupsFragment
import com.example.app.utils.extention.getDrawable
import kotlinx.android.synthetic.main.fragment_social.*

/**
 * A simple [BaseFragment] subclass.
 */
class SocialFragment : BaseFragment(), ViewPager.OnPageChangeListener {
    override fun createLayout(): Int = R.layout.fragment_social

    override fun inject(fragmentComponent: FragmentComponent) = component.inject(this)

    private lateinit var fragmentPagerAdapter: CommonFragmentPagerAdapter

    private val isEvent by lazy { arguments?.getBoolean("isEvent") ?: true }

    override fun bindData() {
        toolbarSocial.setupWithNavController(findNavController(), AppBarConfiguration(findNavController().graph))
        toolbarSocial.title = getString(R.string.hint_search)
        toolbarSocial.navigationIcon = getDrawable(R.drawable.ic_search_discover)
        toolbarSocial.setNavigationOnClickListener { toolbarSocial.callOnClick() }

        viewPagerSocial.addOnPageChangeListener(this)

        toolbarSocial.setOnClickListener(this::onClick)
        imageButtonFilter.setOnClickListener(this::onClick)

        if (this::fragmentPagerAdapter.isInitialized.not()) {
            fragmentPagerAdapter = CommonFragmentPagerAdapter(
                childFragmentManager,
                GroupsFragment(),
                EventsFragment()
            )

            fragmentPagerAdapter.titles = resources.getStringArray(R.array.socialSections)
        }

        viewPagerSocial.adapter = fragmentPagerAdapter
        if (!isEvent) {
            viewPagerSocial.currentItem = 1
        }
        tabLayoutSocial.setupWithViewPager(viewPagerSocial)
    }

    override fun fillData() {}

    private fun onClick(view: View) {
        when (view) {
            toolbarSocial -> {
                val action = SocialFragmentDirections.actionDestSocialToSearchFragment(SearchFragment.TYPE_SOCIAL)
                findNavController().navigate(action)
            }
            imageButtonFilter -> {
                when (viewPagerSocial.currentItem) {
                    1 -> {
                        val fragment =
                            fragmentPagerAdapter.getItem(viewPagerSocial.currentItem) as FilterFragmentHost
                        navigator.loadActivity(
                            IsolatedActivity::class.java,
                            FilterEventsFragment::class.java
                        )
                            .forResult(Common.RequestCode.SOCIAL_FILTER_EVENTS)
                            .addBundle(bundleOf(FeedFilter.KEY to fragment.getFilter()))
                            .start(this)
                    }
                    else -> {
                        val fragment =
                            fragmentPagerAdapter.getItem(viewPagerSocial.currentItem) as FilterFragmentHost
                        navigator.loadActivity(
                            IsolatedActivity::class.java,
                            FilterGroupsFragment::class.java
                        )
                            .forResult(Common.RequestCode.SOCIAL_FILTER_GROUPS)
                            .addBundle(bundleOf(FeedFilter.KEY to fragment.getFilter()))
                            .start(this)
                    }
                }
                val requestCode = when (viewPagerSocial.currentItem) {
                    1 -> Common.RequestCode.SOCIAL_FILTER_EVENTS
                    else -> Common.RequestCode.SOCIAL_FILTER_GROUPS
                }
                val layoutFragment = when (viewPagerSocial.currentItem) {
                    1 -> FilterEventsFragment::class.java
                    else -> FilterGroupsFragment::class.java
                }

            }
        }
    }

    override fun onPageScrollStateChanged(state: Int) {}

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

    override fun onPageSelected(position: Int) {
        val fragment = fragmentPagerAdapter.getItem(position) as FilterFragmentHost
        imageButtonFilter.icon =
            getDrawable(if (fragment.getFilter()?.isPopulated == true) R.drawable.ic_filter_active else R.drawable.ic_filter)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                Common.RequestCode.SOCIAL_FILTER_GROUPS -> {
                    val fragment = fragmentPagerAdapter.getItem(0) as FilterFragmentHost
                    val newFilter = data?.getParcelableExtra<FeedFilter>(FeedFilter.KEY)
                    if (newFilter != null) {
                        imageButtonFilter.icon =
                            getDrawable(if (newFilter.isPopulated) R.drawable.ic_filter_active else R.drawable.ic_filter)
                        fragment.filterUpdated(newFilter)
                    }
                }
                Common.RequestCode.SOCIAL_FILTER_EVENTS -> {
                    val fragment = fragmentPagerAdapter.getItem(1) as FilterFragmentHost
                    val newFilter = data?.getParcelableExtra<FeedFilter>(FeedFilter.KEY)
                    if (newFilter != null) {
                        imageButtonFilter.icon =
                            getDrawable(if (newFilter.isPopulated) R.drawable.ic_filter_active else R.drawable.ic_filter)
                        fragment.filterUpdated(newFilter)
                    }
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}
