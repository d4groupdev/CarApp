package com.example.app.ui.motorshow.adapter

import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import com.example.app.R
import com.example.app.data.model.CarModification
import com.example.app.ui.base.adapter.AdvanceRecycleViewAdapter
import com.example.app.ui.base.adapter.BaseHolder
import com.example.app.ui.base.adapter.OnRecycleItemClick
import com.example.app.utils.extention.inflate
import kotlinx.android.extensions.LayoutContainer

class CarModificationPreviewAdapter (val onRecycleItemClick: OnRecycleItemClick<CarModification>)
    : AdvanceRecycleViewAdapter<CarModificationPreviewAdapter.CarModificationHolder, CarModification>() {

    var type: Int = 0

    override fun createDataHolder(parent: ViewGroup, viewType: Int): CarModificationHolder {

        return CarModificationHolder(parent.inflate(when (type) {
            0 -> R.layout.car_adapter_row_modification_detail
            1 -> R.layout.motorshow_adapter_row_modification
            2 -> R.layout.motorshow_adapter_row_modification_dark

            else -> R.layout.car_adapter_row_modification_detail
        }), onRecycleItemClick)
    }

    override fun onBindDataHolder(holder: CarModificationHolder, position: Int, item: CarModification) {
        holder.textViewModificationName.text = item.name
        holder.textViewEmptyModification.text = item.name

        if (!item.description.isNullOrEmpty())
            holder.textViewDescription.text = item.description

        if (item.id == null) {
            holder.textViewEmptyModification.visibility = View.VISIBLE
            holder.imageViewModification.setImageResource(R.color.imagePlaceholder)
            holder.imageViewModificationIcon.visibility = View.GONE
        } else {
            holder.textViewEmptyModification.visibility = View.GONE

            if (item.id.toInt() == 0) {
                holder.textViewEmptyModification.visibility = View.VISIBLE
                holder.textViewModificationName.visibility = View.GONE
                holder.textViewDescription.visibility = View.GONE
            } else {
                holder.textViewEmptyModification.visibility = View.GONE
                holder.textViewModificationName.visibility = View.VISIBLE
                holder.textViewDescription.visibility = View.VISIBLE
            }

            holder.imageViewModification.visibility = View.VISIBLE
            holder.imageViewModificationIcon.visibility = View.VISIBLE

            holder.imageViewModification.setImageResource(getModificationImage(item.id.toInt()))
            holder.imageViewModificationIcon.setImageResource(getModificationIcon(item.id.toInt()))
        }
    }

    private fun getModificationImage(modificationID: Int): Int {
        return when(modificationID) {
            0 -> R.color.imagePlaceholder
            1 -> R.drawable.mod_background_engine
            2 -> R.drawable.mod_background_drivetrain
            3 -> R.drawable.mod_background_suspension
            4 -> R.drawable.mod_background_wheels
            5 -> R.drawable.mod_background_brakes
            6 -> R.drawable.mod_background_interior
            7 -> R.drawable.mod_background_aerodynamics
            8 -> R.drawable.mod_background_exhaust
            9 -> R.drawable.mod_background_bodywork
            10 -> R.drawable.mod_background_paint
            11 -> R.drawable.mod_background_audio
            12 -> R.drawable.mod_background_transmission
            else -> R.drawable.mod_background_other
        }
    }

    private fun getModificationIcon(modificationID: Int): Int {
        return when(modificationID) {
            0 -> R.drawable.ic_tool
            1 -> R.drawable.ic_mod_engine_large
            2 -> R.drawable.ic_mod_drivetrain_large
            3 -> R.drawable.ic_mod_suspension_large
            4 -> R.drawable.ic_mod_wheels_large
            5 -> R.drawable.ic_mod_brakes_large
            6 -> R.drawable.ic_mod_interior_large
            7 -> R.drawable.ic_mod_aerodynamics_large
            8 -> R.drawable.ic_mod_exhaust_large
            9 -> R.drawable.ic_mod_bodywork_large
            10 -> R.drawable.ic_mod_paint_large
            11 -> R.drawable.ic_mod_audio_large
            12 -> R.drawable.ic_mod_transmission_large
            else -> R.drawable.ic_mod_other_large
        }
    }

    class CarModificationHolder(override val containerView: View, onRecycleItemClick: OnRecycleItemClick<CarModification>)
        : BaseHolder<CarModification>(containerView, onRecycleItemClick), LayoutContainer {

        val imageViewModification: AppCompatImageView = containerView.findViewById(R.id.imageViewModification)
        val imageViewModificationIcon: AppCompatImageView = containerView.findViewById(R.id.imageViewModificationIcon)
        val textViewModificationName: AppCompatTextView = containerView.findViewById(R.id.textViewModificationName)
        val textViewEmptyModification: AppCompatTextView = containerView.findViewById(R.id.textViewEmptyModification)
        val textViewDescription: AppCompatTextView = containerView.findViewById(R.id.textViewDescription)
    }
}