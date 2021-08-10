package com.example.app.ui.social.group.fragment

import android.app.Activity
import android.content.Intent
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.app.R
import com.example.app.core.Common
import com.example.app.data.model.User
import com.example.app.di.component.FragmentComponent
import com.example.app.ui.activity.PlainActivity
import com.example.app.ui.base.BaseFragment
import com.example.app.ui.base.adapter.OnRecycleItemClick
import com.example.app.ui.common.InviteFriendsFragment
import com.example.app.ui.social.group.GroupViewModel
import com.example.app.ui.social.group.adapter.GroupAdminsAdapter
import com.example.app.utils.extention.showAlert
import kotlinx.android.synthetic.main.fragment_group_members.*
import javax.inject.Inject

class GroupMembersFragment : BaseFragment(), Toolbar.OnMenuItemClickListener {
    override fun createLayout(): Int = R.layout.fragment_group_members

    override fun inject(fragmentComponent: FragmentComponent) = component.inject(this)

    private val safeArgs: GroupMembersFragmentArgs by navArgs()

    private val users: Array<User>? by lazy { safeArgs.user }
    private val group by lazy { safeArgs.group }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val groupViewModel by lazy {
        ViewModelProvider(this, viewModelFactory).get(GroupViewModel::class.java)
    }

    private val groupAdminsAdapter: GroupAdminsAdapter by lazy {
        GroupAdminsAdapter(object : OnRecycleItemClick<User>,
            GroupAdminsAdapter.ActivityActionListener {
            override fun onRemove(user: User?, position: Int) {
                showAlert(R.string.app_name, R.string.confirm_remove_group_admin, android.R.string.ok, {
                    groupViewModel.removeInviteGroup(user?.id!!, group?.id!!)
                }, android.R.string.cancel, {})
            }

            override fun onClick(t: User, view: View) {
                if (t.id != session.userId) {
                    if (t.role == 1) {
                        val action = GroupMembersFragmentDirections.actionGlobalToProfileBusiness()
                        action.user = t
                        findNavController().navigate(action)
                    } else {
                        val action = GroupMembersFragmentDirections.actionGlobalToProfile()
                        action.user = t
                        findNavController().navigate(action)
                    }
                }
            }
        }, group?.userId == session.userId, session.userId)
    }

    override fun bindData() {
        toolbarGroupMembers.setupWithNavController(findNavController(), AppBarConfiguration(findNavController().graph))
        toolbarGroupMembers.setOnMenuItemClickListener(this)

        groupViewModel.inviteGroupLiveData.observe(this, {
            showMessage(it.message)
        })

        groupViewModel.removeInviteLiveData.observe(this, { responseBody ->
            showMessage(responseBody.message)
            responseBody.data?.let {
                groupAdminsAdapter.removeItem(it)
            }
        })

        rwGroupMembers.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = groupAdminsAdapter
        }
        groupAdminsAdapter.items = users?.toMutableList()
    }

    override fun fillData() { }

    private lateinit var menuInvite: MenuItem
    override fun createOptionsMenu() {
        if (group?.userId == session.userId) {
            menuInvite = toolbarGroupMembers.menu.add(R.string.add_tag)
            menuInvite.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }
    }

    override fun onMenuItemClick(item: MenuItem?) = when(item) {
        menuInvite -> {
            navigator.loadActivity(PlainActivity::class.java, InviteFriendsFragment::class.java)
                .addBundle(
                    bundleOf(
                        "title" to getString(R.string.invite_friends),
                        "type" to InviteFriendsFragment.TYPE_GROUP,
                        InviteFriendsFragment.SELECTED to users
                    )
                )
                .forResult(Common.RequestCode.GROUP_INVITE_ADMINS)
                .start(this)
            true
        }
        else -> true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                Common.RequestCode.GROUP_INVITE_ADMINS -> {
                    val users = data?.getParcelableArrayListExtra<User>("users")
                    users?.let {
                        groupViewModel.inviteGroup(users.joinToString(",") { it.id ?: "" }, group?.id!!)
                    }
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}