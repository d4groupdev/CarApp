package com.example.app.ui.business.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import com.example.app.R
import com.example.app.data.model.Sponsorship
import com.example.app.data.model.User
import com.example.app.ui.adapters.SponsorshipAdapter
import com.example.app.ui.base.adapter.AdvanceRecycleViewAdapter
import com.example.app.ui.base.adapter.BaseHolder
import com.example.app.ui.base.adapter.OnRecycleItemClick
import com.example.app.ui.views.CircleImageView
import com.example.app.utils.extention.loadProfilePicture
import kotlinx.android.extensions.LayoutContainer

class BusinessSponsorshipAdapter(private val listActionListener: BusinessSponsorshipActionListener, private val user: User?):
    AdvanceRecycleViewAdapter<BusinessSponsorshipAdapter.BusinessSponsorshipHolder, Sponsorship>() {

    interface BusinessSponsorshipActionListener: OnRecycleItemClick<Sponsorship> {
        fun onProfileClick(user: User)
        fun onCancel(sponsorship: Sponsorship)
    }

    override fun createDataHolder(parent: ViewGroup, viewType: Int): BusinessSponsorshipHolder {
        return BusinessSponsorshipHolder(makeView(parent, R.layout.adapter_row_business_sponsorship), listActionListener, user)
    }

    override fun onBindDataHolder(holder: BusinessSponsorshipHolder, position: Int, item: Sponsorship) {
        holder.imageViewUserAvatar.loadProfilePicture(item.userProfileImage)
        holder.textViewPartnerName.text = item.userName
        holder.vehiclesAdapter.items = item.cars
    }

    class BusinessSponsorshipHolder(override val containerView: View, val listener: BusinessSponsorshipActionListener, user: User?) :
        BaseHolder<Sponsorship>(containerView, listener), LayoutContainer, View.OnClickListener {

        val imageViewUserAvatar: CircleImageView = containerView.findViewById(R.id.imageViewUserAvatar)
        val textViewPartnerName: MaterialTextView = containerView.findViewById(R.id.textViewPartnerName)
        private val recyclerViewVehicles: RecyclerView = containerView.findViewById(R.id.recyclerViewVehicles)

        private val adapterListener by lazy {
            object : SponsorshipAdapter.SponsorshipActionListener {
                override fun onCancel(sponsorship: Sponsorship) {
                    listener.onCancel(sponsorship)
                }

                override fun onClick(t: Sponsorship, view: View) {
                    listener.onClick(t, view)
                }

                override fun onEdit(sponsorship: Sponsorship) {

                }
            }
        }

        val vehiclesAdapter by lazy { SponsorshipAdapter(adapterListener, isSub = true, user = user, isCurrent = true) }

        init {
            recyclerViewVehicles.apply {
                layoutManager = LinearLayoutManager(containerView.context)
                adapter = vehiclesAdapter
            }

            imageViewUserAvatar.setOnClickListener(this::onClick)
            textViewPartnerName.setOnClickListener(this::onClick)
        }

        override fun onClick(v: View) {
            when (v) {
                imageViewUserAvatar, textViewPartnerName -> {
                    listener.onProfileClick(current.user)
                }
                else -> super.onClick(v)
            }
        }
    }
}