package com.example.app.ui.motorshow.fragment


import android.app.Activity
import android.content.Intent
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.app.R
import com.example.app.core.Common
import com.example.app.data.model.CarModification
import com.example.app.di.component.FragmentComponent
import com.example.app.ui.activity.PlainActivity
import com.example.app.ui.adapters.CarModificationAdapter
import com.example.app.ui.authentication.AuthenticationViewModel
import com.example.app.ui.base.BaseFragment
import com.example.app.ui.base.adapter.AbstractSelectableAdapter
import com.example.app.ui.base.adapter.OnRecycleItemClick
import com.example.app.ui.base.adapter.OnSelectionChangeListener
import com.example.app.ui.feed.fragment.MiSingl
import kotlinx.android.synthetic.main.fragment_car_modification.*
import javax.inject.Inject

class CarModificationFragment : BaseFragment(), OnSelectionChangeListener<CarModification>,
    OnRecycleItemClick<CarModification> {

    override fun createLayout(): Int = R.layout.fragment_car_modification

    override fun inject(fragmentComponent: FragmentComponent) = component.inject(this)

    private var carModificationAdapter: CarModificationAdapter? = null

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val authenticationViewModel by lazy {
        ViewModelProvider(this, viewModelFactory).get(AuthenticationViewModel::class.java)
    }

    private lateinit var selectedModifications: ArrayList<CarModification>

    private fun updateModificationsList(it: ArrayList<CarModification>) {
        MiSingl.getInstance()?.carModif?.forEach { carModification ->
            if (it.contains(carModification)) {
                val i = it.indexOf(carModification)
                carModification.isSelected = true
                it[i] = carModification
            }

        }
        carModificationAdapter?.items = it
    }

    override fun bindData() {
        setHasOptionsMenu(true)
        toolbarModification.title = getString(R.string.add_mods)
        toolbar.setToolbar(toolbarModification)
        toolbar.showBackButton(true)

        authenticationViewModel.modificationsLiveData.observe(this, {
            updateModificationsList(it.data!!)
        })

        if (carModificationAdapter == null) {
            carModificationAdapter = CarModificationAdapter(AbstractSelectableAdapter.MULTI, this)
        }

        recycleViewModification.layoutManager = LinearLayoutManager(context)
        recycleViewModification.adapter = carModificationAdapter
        carModificationAdapter?.addSelectionChangeListener(this)

        selectedModifications =
            arguments?.getParcelableArrayList("modification") ?: ArrayList()
    }

    override fun fillData() {
        authenticationViewModel.getModificationTypes()
    }

    private var menuSave: MenuItem? = null

    override fun onSelectionChange(t: CarModification, isSelected: Boolean) {
    }

    override fun onClick(t: CarModification, view: View) {
        navigator.loadActivity(
            PlainActivity::class.java,
            CarModificationDetailsFragment::class.java
        )
            .addBundle(
                bundleOf(
                    "modification" to t,
                    "position" to carModificationAdapter?.items?.indexOf(t)
                )
            )
            .forResult(Common.RequestCode.CREATE_VEHICLE_MODIFICATION)
            .start()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == Common.RequestCode.CREATE_VEHICLE_MODIFICATION && data != null) {
            val position = data.getIntExtra("position", -1)
            val modification = data.getParcelableExtra<CarModification>("modification")
            when (resultCode) {
                Activity.RESULT_OK -> {
                    modification?.isSelected = true
                }
                Activity.RESULT_CANCELED -> {
                    modification?.isSelected = false
                }
            }
            if (position != -1) {
                carModificationAdapter!!.items?.set(position, modification!!)
                carModificationAdapter!!.notifyDataSetChanged()
            }
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        menuSave = menu.add(getString(R.string.save))
        menuSave?.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item) {
            menuSave -> {
                requireActivity().setResult(Activity.RESULT_OK, Intent().also {
                    it.putParcelableArrayListExtra(
                        "modification",
                        carModificationAdapter?.selectedItems
                    )

                    MiSingl.getInstance()?.carModif = carModificationAdapter?.selectedItems
                })
                navigator.finish()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }
}
