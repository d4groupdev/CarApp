package com.example.app.ui.business.fragment

import android.app.Activity
import android.content.Intent
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.app.R
import com.example.app.core.Common
import com.example.app.data.model.*
import com.example.app.di.component.FragmentComponent
import com.example.app.ui.activity.PlainActivity
import com.example.app.ui.alerts.AlertsViewModel
import com.example.app.ui.base.BaseFragment
import com.example.app.ui.business.adapter.BusinessSponsorshipAdapter
import com.example.app.ui.motorshow.MotorShowViewModel
import com.example.app.ui.profile.ProfileViewModel
import com.example.app.ui.profile.fragment.InviteSponsorVehicleListFragment
import com.example.app.utils.extention.showAlert
import kotlinx.android.synthetic.main.fragment_partnership_active.*
import javax.inject.Inject

class PartnershipActiveFragment : BaseFragment(),
    BusinessSponsorshipAdapter.BusinessSponsorshipActionListener, Toolbar.OnMenuItemClickListener {

    override fun createLayout(): Int = R.layout.fragment_partnership_active

    override fun inject(fragmentComponent: FragmentComponent) = component.inject(this)

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val profileViewModel by lazy {
        ViewModelProvider(this, viewModelFactory).get(ProfileViewModel::class.java)
    }
    private val alertsViewModel by lazy {
        ViewModelProvider(this, viewModelFactory).get(AlertsViewModel::class.java)
    }
    private val motorShowViewModel by lazy {
        ViewModelProvider(this, viewModelFactory).get(MotorShowViewModel::class.java)
    }

    private val isToolbar by lazy { arguments?.getBoolean("toolbar") ?: false }
    private val user by lazy { arguments?.getParcelable<User>(User.KEY) }

    private val listAdapter by lazy { BusinessSponsorshipAdapter(this, session.user) }

    override fun bindData() {
        if (isToolbar) {
            appBar.visibility = View.VISIBLE
            toolbarActivePartnership.setupWithNavController(findNavController(), AppBarConfiguration(findNavController().graph))
            toolbarActivePartnership.setOnMenuItemClickListener(this)
        }

        profileViewModel.businessSponsorshipLiveData.observe(this, {
            listAdapter.items = it.data
            if (it.data?.size == 0)
                listAdapter.errorMessage = it.message
        })

        alertsViewModel.notificationActionLiveData.observe(this, {
            if (it.responseCode == 1) {
                profileViewModel.loadBusinessSponsorship(BodyBuilder(userId = user?.id))
            }
        })

        motorShowViewModel.inviteSponsorLiveData.observe(this, {
            showMessage(it.message)
        })

        recyclerViewPartnership.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = listAdapter
        }
    }

    override fun fillData() {
        showLoader()
        profileViewModel.loadBusinessSponsorship(BodyBuilder(userId = user?.id))
    }

    override fun onProfileClick(user: User) {
        if (user.id != session.userId) {
            if (user.isBusiness) {
                val action = PartnershipActiveFragmentDirections.actionGlobalToProfileBusiness()
                action.user = user
                findNavController().navigate(action)
            } else {
                val action = PartnershipActiveFragmentDirections.actionGlobalToProfile()
                action.user = user
                findNavController().navigate(action)
            }
        }
    }

    override fun onCancel(sponsorship: Sponsorship) {
        showAlert(R.string.app_name, R.string.are_you_sure_remove_partnership, android.R.string.ok, {
            alertsViewModel.sponsorAction(Alerts(refferenceId = sponsorship.id), 0)
        }, android.R.string.cancel, {})
    }

    override fun onClick(sponsorship: Sponsorship, view: View) {
        val action = PartnershipActiveFragmentDirections.actionPartnershipToManagement()
        action.sponsorship = sponsorship
        findNavController().navigate(action)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                Common.RequestCode.ADD_SPONSOR_FROM_DETAILS -> {
                    val sponsorUser = data?.getParcelableExtra<Sponsorship>(Sponsorship.KEY)
                    val vehicle = data?.getParcelableExtra<Vehicle>(Vehicle.KEY)

                    sponsorUser?.let {
                        showLoader()
                        sponsorUser.businessId = user?.id
                        sponsorUser.vehicleId = vehicle?.id
                        motorShowViewModel.inviteSponsor(sponsorUser)
                    }
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private lateinit var menuInvite: MenuItem
    override fun createOptionsMenu() {
        if (session.user?.isBusiness == false) {
            menuInvite = toolbarActivePartnership.menu.add(R.string.invite)
            menuInvite.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }
    }

    override fun onMenuItemClick(item: MenuItem?) = when(item) {
        menuInvite -> {
            navigator.loadActivity(PlainActivity::class.java, InviteSponsorVehicleListFragment::class.java)
                .addBundle(bundleOf(User.KEY to user))
                .forResult(Common.RequestCode.ADD_SPONSOR_FROM_DETAILS)
                .start(this)
            true
        }
        else -> true
    }

}
