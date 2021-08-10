package com.example.app.ui.profile.fragment

import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.app.R
import com.example.app.data.model.User
import com.example.app.di.component.FragmentComponent
import com.example.app.ui.base.BaseFragment
import com.example.app.ui.profile.ProfileViewModel
import com.example.app.ui.social.group.adapter.CreateGroupAdminsAdapter
import javax.inject.Inject

/**
 * A simple [BaseFragment] subclass.
 */
class FollowingFragment : BaseFragment(), CreateGroupAdminsAdapter.FollowActionListener {

    override fun createLayout(): Int = R.layout.common_fragment_people

    override fun inject(fragmentComponent: FragmentComponent) = component.inject(this)

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val profileViewModel by lazy {
        ViewModelProvider(this, viewModelFactory).get(ProfileViewModel::class.java)
    }

    private val peopleAdapter: CreateGroupAdminsAdapter by lazy { CreateGroupAdminsAdapter(
        CreateGroupAdminsAdapter.SELECTION_FOLLOW,
        this,
        ""
    ) }

    private val user by lazy { arguments?.getParcelable(User.KEY) as? User }
    private val listType by lazy { arguments?.getInt(LIST_TYPE, TYPE_PROFILE_FOLLOWER) }

    override fun bindData() {
        if (user?.role == 0 && listType == TYPE_PROFILE_FOLLOWER) {
            profileViewModel.followersListLiveData
        } else if (user?.role == 0 && listType == TYPE_PROFILE_FOLLOWING) {
            profileViewModel.followingListLiveData
        } else {
            profileViewModel.followingBusinessListLiveData
        }.observe(this, {
            peopleAdapter.items = it.data
        }, { e, _ ->
            peopleAdapter.errorMessage = e.message ?: ""
            false
        })

        profileViewModel.followUnfollowLiveData.observe(this, {
            peopleAdapter.notifyDataSetChanged()
        }, { _, _ ->
            true
        })
    }

    override fun fillData() {
        loadData()
    }

    private fun loadData() {
        if (user?.role == 1 && listType == TYPE_PROFILE_FOLLOWER) {
            profileViewModel.loadFollowersBusinessList(user!!, 1)
        } else if (user?.role == 1 && listType == TYPE_PROFILE_FOLLOWING) {
            profileViewModel.loadFollowingBusinessList(user!!, 1)
        } else if (user?.role == 0 && listType == TYPE_PROFILE_FOLLOWER) {
            profileViewModel.loadFollowersList(user!!, 1)
        } else if (user?.role == 0 && listType == TYPE_PROFILE_FOLLOWING) {
            profileViewModel.loadFollowingList(user!!, 1)
        }
    }

    override fun onClickFollow(user: User) {
        profileViewModel.follow(true, user.id!!)
    }

    override fun onClick(user: User, view: View) {
        if (session.userId != user.id) {
            if (user.role == 1) {
                val action = FollowerAndFollowingFragmentDirections.actionGlobalToProfileBusiness()
                action.user = user
                findNavController().navigate(action)
            } else {
                val action = FollowerAndFollowingFragmentDirections.actionGlobalToProfile()
                action.user = user
                findNavController().navigate(action)
            }
        }
    }

    companion object {
        const val LIST_TYPE = "list_type"

        const val TYPE_PROFILE_FOLLOWER = 1
        const val TYPE_PROFILE_FOLLOWING = 2
    }

}
