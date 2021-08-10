package com.example.app.ui.alerts.fragment

import android.util.Log
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.app.R
import com.example.app.data.model.*
import com.example.app.di.component.FragmentComponent
import com.example.app.twilio.chat.TwilioChatClient
import com.example.app.ui.alerts.AlertsViewModel
import com.example.app.ui.alerts.adapter.NotificationsAdapter
import com.example.app.ui.base.BaseFragment
import com.example.app.ui.messaging.fragment.MessageAndNotificationFragmentDirections
import com.example.app.ui.motorshow.MotorShowViewModel
import com.example.app.ui.profile.ProfileViewModel
import com.example.app.ui.social.event.EventViewModel
import com.example.app.ui.social.group.GroupViewModel
import com.example.app.utils.EndlessRecyclerViewScrollListener
import com.twilio.chat.ErrorInfo
import com.twilio.chat.StatusListener
import kotlinx.android.synthetic.main.fragment_notifications.*
import javax.inject.Inject

class NotificationsFragment : BaseFragment(), NotificationsAdapter.NotificationsListener {
    override fun createLayout(): Int = R.layout.fragment_notifications

    override fun inject(fragmentComponent: FragmentComponent) = component.inject(this)

    private val notificationsAdapter by lazy {
        NotificationsAdapter(this)
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var twilioChatClient: TwilioChatClient

    private val feedViewModel: AlertsViewModel by lazy {
        ViewModelProvider(this, viewModelFactory).get(AlertsViewModel::class.java)
    }

    private val eventViewModel: EventViewModel by lazy {
        ViewModelProvider(this, viewModelFactory).get(EventViewModel::class.java)
    }

    private val groupViewModel: GroupViewModel by lazy {
        ViewModelProvider(this, viewModelFactory).get(GroupViewModel::class.java)
    }
    private val motorShowViewModel: MotorShowViewModel by lazy {
        ViewModelProvider(this, viewModelFactory).get(MotorShowViewModel::class.java)
    }
    private val profileViewModel: ProfileViewModel by lazy {
        ViewModelProvider(this, viewModelFactory).get(ProfileViewModel::class.java)
    }

    var filter: FeedFilter = FeedFilter()

    override fun bindData() {
        recyclerViewNotifications.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = notificationsAdapter
            addOnScrollListener(object :
                EndlessRecyclerViewScrollListener(layoutManager as LinearLayoutManager) {
                override fun onLoadMore(page: Int, totalItemsCount: Int) {
                    feedViewModel.loadNotification(page, filter)
                }

            })
        }

        feedViewModel.notificationActionLiveData.observeOwner(this, {
            if (it.data != null)
                notificationsAdapter.removeItem(it.data)
        })

        groupViewModel.groupAdminActionLiveData.observeOwner(this, {
            if (it.data != null)
                notificationsAdapter.removeItem(it.data)
        })

        eventViewModel.eventActionLiveData.observeOwner(this, { responseBody ->
            if (responseBody.data?.alerts != null)
                feedViewModel.removeAlert(responseBody.data.alerts!!)
        })

        groupViewModel.groupActionLiveData.observeOwner(this, {
            if (it.data?.alerts != null) {
                feedViewModel.removeAlert(it.data.alerts!!)

                if (it.data.joinStatus == Group.STATUS_JOINED || it.data.joinStatus == Group.STATUS_REQUESTED) {
                    if (it.data.id != null)
                        try {
                            twilioChatClient.loadChannel("grp_" + it.data.id)
                                .subscribe { c, t ->
                                    c?.join(object : StatusListener() {
                                        override fun onSuccess() {
                                        }

                                        override fun onError(errorInfo: ErrorInfo?) {
                                            super.onError(errorInfo)
                                        }

                                    })
                                    if (t != null)
                                        Log.d("Testing", " Join Fail ", t)
                                }
                        } catch (e: Exception) {

                        }
                }
            }

        })

        groupViewModel.groupAdminActionLiveData.observeOwner(this, {

        })

        motorShowViewModel.makeOfferActionLiveData.observeOwner(this, {
            if (it.data != null)
                feedViewModel.removeAlert(it.data)
        })

        profileViewModel.followActionLiveData.observeOwner(this, {
            if (it.data != null)
                feedViewModel.removeAlert(it.data)
        })
    }

    override fun fillData() {
        feedViewModel.loadNotification(1, filter)
    }

    override fun onImage(alerts: Alerts, v: View) {
        val action =
            MessageAndNotificationFragmentDirections.actionGlobalToGroupDetailsFragment()
        action.group = Group(id = alerts.refferenceId)
        findNavController().navigate(action)
    }


    override fun onClick(t: Alerts, view: View) {
        when (t.notificationTag) {
            Alerts.SPONSORS_REQUEST_BUSINESS -> {

                if (session.user?.isBusiness == true) {
                    val action =
                        MessageAndNotificationFragmentDirections.actionMessageAndNotificationFragmentToSponsorInviteFragment()
                    action.sponsorship =
                        Sponsorship(id = t.refferenceId, userId = t.userId, coverImage = t.image)
                    action.sponsorshipId = t.refferenceId
                    action.userId = t.userId
                    action.messages = t.message
                    findNavController().navigate(action)
                }
            }

            Alerts.TAG_LIKE_COMMENT_FEED, Alerts.TAG_LIKE_FEED, Alerts.TAG_COMMENT_FEED,

            Alerts.TAG_LIKE_BUSINESS_REVIEW, Alerts.TAG_LIKE_COMMENT_BUSINESS_REVIEW,


            Alerts.TAG_LIKE_REVIEW, Alerts.TAG_LIKE_COMMENT_REVIEW, Alerts.TAG_REVIEW_POST,
            Alerts.TAG_COMMENT_REVIEW, Alerts.TAG_REPORT_REVIEW -> {
            }

            Alerts.TAG_LIKE_COMMENT_EVENT, Alerts.TAG_LIKE_EVENT, Alerts.TAG_COMMENT_EVENT,
            Alerts.EVENT_INVITATION, Alerts.EVENT_INVITATION_PRIVATE,
            Alerts.EVENT_INVITATION_PUBLIC, Alerts.EVENT_INTRESS, Alerts.EVENT_RSVP -> {
                val action =
                    MessageAndNotificationFragmentDirections.actionGlobalToEventDetailFragment()
                action.event = Event(id = t.refferenceId)
                findNavController().navigate(action)
            }

            Alerts.GROUP_INVITATION, Alerts.GROUP_JOIN_REQUEST,
            Alerts.TAG_COMMENT_GROUP, Alerts.TAG_LIKE_GROUP, Alerts.GROUP_JOIN -> {
                val action =
                    MessageAndNotificationFragmentDirections.actionGlobalToGroupDetailsFragment()
                action.group = Group(id = t.refferenceId)
                findNavController().navigate(action)
            }

            Alerts.GROUP_HOST -> {
                SingletonNotif.getInstance().myRef = t.refferenceId
                SingletonNotif.getInstance().myAlerts = t
                val notificationGroupIsHost = NotificationGroupIsHost.newInstance()
                notificationGroupIsHost.show(
                    childFragmentManager,
                    NotificationGroupIsHost::class.java.canonicalName
                )
            }

            Alerts.EVENT_HOST -> {
                SingletonNotif.getInstance().myRef = t.refferenceId
                SingletonNotif.getInstance().myAlerts = t
                val notificationGroupIsHost = NotificationEventIsHost.newInstance()
                notificationGroupIsHost.show(
                    childFragmentManager,
                    NotificationGroupIsHost::class.java.canonicalName
                )
            }

            Alerts.TAG_LIKE_VEHICLE, Alerts.TAG_COMMENT_VEHICLE, Alerts.TAG_LIKE_COMMENT_VEHICLE -> {
                val action =
                    MessageAndNotificationFragmentDirections.actionGlobalVehicleDetails()
                action.vehicle = Vehicle(id = t.refferenceId)
                findNavController().navigate(action)
            }
            Alerts.TAG_FOLLOW -> {
                val action = MessageAndNotificationFragmentDirections.actionGlobalToProfile()
                action.user = User(id = t.otherUserId)
                findNavController().navigate(action)

            }
        }

    }

    fun onAction(
        action: Int,
        myRef: String,
        myAlerts: Alerts
    ) {
        when (action) {
            Group.GROUP_ACCEPT -> groupViewModel.groupAction(
                Group(
                    id = myRef,
                    joinStatus = Group.STATUS_NOT_JOINED,
                    alerts = myAlerts
                )
            )
            Group.GROUP_DECLINE -> groupViewModel.groupAction(
                Group(
                    id = myRef,
                    alerts = myAlerts
                )
            )
            Group.GROUP_CANCEL -> {
            }
        }

    }

    fun onActionEvent(
        action: Int,
        myRef: String,
        myAlerts: Alerts
    ) {
        when (action) {
            Event.EVENT_ACCEPT -> eventViewModel.eventAction(
                Event(id = myRef, alerts = myAlerts),
                Event.ACCEPTED
            )
            Event.EVENT_DECLINE -> eventViewModel.eventAction(
                Event(id = myRef, alerts = myAlerts),
                Event.REJECTED
            )
            Event.EVENT_CANCEL -> {
            }
        }
    }
}