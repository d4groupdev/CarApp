package com.example.app.ui.motorshow.fragment


import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Build
import android.text.format.DateUtils
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.Toolbar
import androidx.core.os.bundleOf
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.app.R
import com.example.app.core.Common
import com.example.app.data.model.*
import com.example.app.di.component.FragmentComponent
import com.example.app.exception.ServerException
import com.example.app.ui.activity.PlainActivity
import com.example.app.ui.base.BaseFragment
import com.example.app.ui.base.adapter.OnRecycleItemClick
import com.example.app.ui.common.ImagePreviewActivity
import com.example.app.ui.common.Reporter
import com.example.app.ui.common.SponsorshipListFragment
import com.example.app.ui.common.adapter.CarouselPagerAdapter
import com.example.app.ui.dialog.SuccessAlertDialog
import com.example.app.ui.feed.FeedViewModel
import com.example.app.ui.feed.adapter.FeedCommentAdapter
import com.example.app.ui.manager.ShareManager
import com.example.app.ui.motorshow.MotorShowViewModel
import com.example.app.ui.motorshow.adapter.CarModificationPreviewAdapter
import com.example.app.ui.motorshow.adapter.CarSpecificationDetailAdapter
import com.example.app.ui.motorshow.adapter.MomentPreviewListAdapter
import com.example.app.ui.profile.ProfileViewModel
import com.example.app.ui.profile.adapter.SponsorsPreviewAdapter
import com.example.app.utils.extention.*
import kotlinx.android.synthetic.main.fragment_feed_details.*
import kotlinx.android.synthetic.main.fragment_motor_show_detail.*
import kotlinx.android.synthetic.main.fragment_motor_show_detail.blockFeatured
import kotlinx.android.synthetic.main.fragment_motor_show_detail.buttonFollow
import kotlinx.android.synthetic.main.fragment_motor_show_detail.buttonShare
import kotlinx.android.synthetic.main.fragment_motor_show_detail.clAddComment
import kotlinx.android.synthetic.main.fragment_motor_show_detail.profileImage
import kotlinx.android.synthetic.main.fragment_motor_show_detail.profileImageComment
import kotlinx.android.synthetic.main.fragment_motor_show_detail.swipeRefresh
import kotlinx.android.synthetic.main.fragment_motor_show_detail.tabLayoutImageIndicator
import kotlinx.android.synthetic.main.fragment_motor_show_detail.textViewCommentCount
import kotlinx.android.synthetic.main.fragment_motor_show_detail.textViewName
import kotlinx.android.synthetic.main.fragment_profile.collapsingToolbar
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList
import kotlin.math.pow

class MotorShowDetailFragment : BaseFragment(), SwipeRefreshLayout.OnRefreshListener,
    FeedCommentAdapter.CommentUpdateListener, Toolbar.OnMenuItemClickListener {

    override fun createLayout(): Int = R.layout.fragment_motor_show_detail

    override fun inject(fragmentComponent: FragmentComponent) = component.inject(this)

    private lateinit var car: Vehicle
    private var story: Story? = null

    private val safeArgs: MotorShowDetailFragmentArgs by navArgs()

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val motorShowViewModel by lazy {
        ViewModelProvider(this, viewModelFactory).get(MotorShowViewModel::class.java)
    }
    private val profileViewModel by lazy {
        ViewModelProvider(this, viewModelFactory).get(ProfileViewModel::class.java)
    }
    private val feedViewModel by lazy {
        ViewModelProvider(this, viewModelFactory).get(FeedViewModel::class.java)
    }

    private val modificationAdapter: CarModificationPreviewAdapter by lazy {
        CarModificationPreviewAdapter(object : OnRecycleItemClick<CarModification> {
            override fun onClick(t: CarModification, view: View) {
                if (t.description.isNullOrEmpty().not()) {
                    val action =
                        MotorShowDetailFragmentDirections.actionVehicleDetailsToDestVehicleMod(t)
                    findNavController().navigate(action)
                }
            }
        })
    }

    private val commentAdapter by lazy { FeedCommentAdapter(requireContext(), this) }

    private val momentsAdapterPreview: MomentPreviewListAdapter by lazy {
        MomentPreviewListAdapter(object : MomentPreviewListAdapter.ActivityActionListener {
            override fun onClick(t: Moment, view: View) {
                val action = MotorShowDetailFragmentDirections.actionGlobalMotorShowDetailMoment(t)
                findNavController().navigate(action)
            }

            override fun onRemove(moment: Moment?, position: Int) {

            }
        })
    }

    private val sponsorsAdapter by lazy {
        SponsorsPreviewAdapter(object : OnRecycleItemClick<Sponsorship> {
            override fun onClick(t: Sponsorship, view: View) {
                if (t.businessId != session.userId) {
                    val action =
                        MotorShowDetailFragmentDirections.actionDestVehicleDetailsToDestPartnershipManagement()
                    action.sponsorship = t
                    findNavController().navigate(action)
                }
            }

        })
    }

    private val carSpecificationDetailAdapter: CarSpecificationDetailAdapter by lazy {
        CarSpecificationDetailAdapter().apply {
            layoutViewType =
                if (isFeatured) R.layout.car_adapter_row_car_specification_dark else R.layout.car_adapter_row_car_specification
        }
    }

    var measuredWidth = 0
    var isFeatured: Boolean = false
    var vibrantColor: Int = 0

    @Inject
    lateinit var shareManage: ShareManager

    private val isMe by lazy {
        car.userId == session.user?.id
    }

    lateinit var adapter: CarouselPagerAdapter

    override fun bindData() {
        story = safeArgs.story
        car = (if (story != null) story?.vehiclesDetail else safeArgs.vehicle) ?: Vehicle()

        collapsingToolbar.setupWithNavController(
            toolbarMotorShowDetail,
            findNavController(),
            AppBarConfiguration(findNavController().graph)
        )

        if (story != null) {
            imageButtonBookmark.visibility = View.GONE
        }

        toolbarMotorShowDetail.setOnMenuItemClickListener(this)

        buttonMoments.setOnClickListener(this::onClick)
        buttonAddMoment.setOnClickListener(this::onClick)
        buttonSponsors.setOnClickListener(this::onClick)
        buttonComments.setOnClickListener(this::onClick)
        textViewLike.setOnClickListener(this::onClick)
        buttonMessage.setOnClickListener(this::onClick)
        textViewCommentCount.setOnClickListener(this::onClick)
        buttonShare.setOnClickListener(this::onClick)
        textViewName.setOnClickListener(this::onClick)
        profileImage.setOnClickListener(this::onClick)
        imageButtonBookmark.setOnClickListener(this::onClick)
        clAddComment.setOnClickListener(this::onClick)
        buttonFollow.setOnClickListener(this::onClick)

        swipeRefresh.setOnRefreshListener(this)

        recycleViewSpecification.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = carSpecificationDetailAdapter
        }

        recyclerViewComment.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = commentAdapter
            itemAnimator = DefaultItemAnimator()
        }

        adapter = CarouselPagerAdapter(arrayListOf()) { imageView, media ->

            if (!isMe) {
                Log.d("lsdl234234", "type " + media.type)
                Log.d("lsdl234234", "media " + media.media.toString())
                Log.d("lsdl234234", "caption " + media.caption)
                Log.d("lsdl234234", "isCover " + media.isCover)
                imageView.transitionName = "image"
                navigator.loadActivity(ImagePreviewActivity::class.java)
                    .addBundle(
                        bundleOf(
                            Media.KEY to media,
                            "is_preview" to true,
                            "youtubeLink" to if (media.type == "V") story?.youtube else ""
                        )
                    )
                    .addSharedElements(
                        arrayListOf(
                            androidx.core.util.Pair.create(
                                imageView,
                                "image"
                            )
                        ) as List<androidx.core.util.Pair<View, String>>
                    )
                    .start()
            } else {
                if (media.type == Media.MEDIA_TYPE_VIDEO) {
                    imageView.transitionName = "image"
                    navigator.loadActivity(ImagePreviewActivity::class.java)
                        .addBundle(
                            bundleOf(
                                Media.KEY to media,
                                "is_preview" to true,
                                "youtubeLink" to story?.youtube
                            )
                        )
                        .addSharedElements(
                            arrayListOf(
                                androidx.core.util.Pair.create(
                                    imageView,
                                    "image"
                                )
                            ) as List<androidx.core.util.Pair<View, String>>
                        )
                        .start()

                } else {
                    Log.d("lsdl234234", "---------- ")
                    startActivityForResult(
                        Intent(
                            activity,
                            ImagePreviewActivity::class.java
                        ).apply {
                            putExtras(bundleOf(Media.KEY to media.also { media ->
                                media.referenceId = car.id
                            },
                                "action_name" to getString(R.string.garage_cover),
                                "action_dec" to getString(R.string.image_will_be_used_as_vehicle_cover),
                                "is_preview" to true))
                        }, Common.RequestCode.IMAGE_PREVIEW
                    )
                }
            }
        }

        viewPagerMedia.adapter = adapter
        tabLayoutImageIndicator.setupWithViewPager(viewPagerMedia)

        tagLayout.isRemovable = false

        // Modification adapter setup
        recycleViewModification.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = modificationAdapter
        }
        modificationAdapter.type = if (isFeatured) 2 else 1

        recycleViewMoments.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = momentsAdapterPreview
        }

        recycleViewSponsors.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = sponsorsAdapter
        }

        tagLayout.clickCallback = { tag ->
            val action = MotorShowDetailFragmentDirections.actionGlobalToHashtagList(tag)
            findNavController().navigate(action)
        }
    }

    override fun fillData() {
        profileImageComment.load(session.user?.profileImage)

        onRefresh()

        motorShowViewModel.likeUnlikeLiveData.observe(this, {
            if (textViewLike.tag != null) {
                car.isLike = textViewLike.tag as Boolean

                if (car.isLike!!)
                    car.likeCount = (car.likeCount ?: 0) + 1
                else car.likeCount = (car.likeCount ?: 0) - 1

                textViewLike.text = car.likeCount.default(0).toString()
                textViewLike.tag = true

                changeLikeStatus(car.isLike!!)
            }

        }, { _, _ ->
            car.isLike = (textViewLike.tag as Boolean).not()
            textViewLike.tag = car.isLike!!

            changeLikeStatus(car.isLike!!)

            true
        })

        motorShowViewModel.favoriteLiveData.observe(this, {
            if (imageButtonBookmark.tag != null) {
                car.isFavorite = imageButtonBookmark.tag as Boolean
                changeBookmarkStatus(imageButtonBookmark.tag as Boolean)
            }
        }, { _, _ ->
            car.isFavorite = (imageButtonBookmark.tag as Boolean).not()
            imageButtonBookmark.tag = car.isFavorite
            changeBookmarkStatus(imageButtonBookmark.tag as Boolean)
            true
        })

        profileViewModel.followUnfollowLiveData.observe(this, {
            car.isFollowing = !(car.isFollowing ?: true)
            checkOwnerBlock()
        }, { _, _ ->
            car.isFollowing = !(car.isFollowing ?: false)
            checkOwnerBlock()
            true
        })

        motorShowViewModel.vehicleDetailLiveData.observe(this, {
            if (it.data != null) {
                this.car = it.data
                setDetails()
            }
        })

        motorShowViewModel.moveVehicleLiveData.observe(this, {
            navigator.goBack()
        })

        profileViewModel.deleteVehicleLiveData.observeOwner(this, {
            findNavController().popBackStack()
        })

        motorShowViewModel.addRemoveMotorShowLiveData.observeOwner(this, {
            this.car.isMotorshow = this.car.isMotorshow?.not()
            showMessage(it.message)
        })

        motorShowViewModel.reportLiveData.observeOwner(this, {
            SuccessAlertDialog(requireContext())
                .also { dialog ->
                    dialog.setTitle(R.string.reported)
                    dialog.setMessage(R.string.report_vehicle_thank_you_message)
                    dialog.setPositiveButton(R.string.done) {
                        dialog.dismiss()
                    }
                }
                .show()
        })

        motorShowViewModel.storyDetailLiveData.observe(this, {
            if (it.data != null) {
                story = it.data
                story?.requestType = "story"
                setDetails()
            }
        })

        motorShowViewModel.commentsListLiveData.observe(this, {
            if (it.data != null) {
                showLoader()
                commentAdapter.originalItems = it.data
            }

        }, { throwable, _ ->
            if (throwable is ServerException) return@observe false

            true
        })

        motorShowViewModel.commentLiveData.observe(this, {
            if (it.data != null) {
                if (it.data.parentCommentId.isNullOrEmpty() || it.data.parentCommentId == "0") {
                    commentAdapter.addItem(it.data)
                } else {
                    val find =
                        commentAdapter.items?.find { comment -> comment.id == it.data.parentCommentId }
                    if (find != null) {
                        if (find.subComments == null)
                            find.subComments = ArrayList()

                        find.subComments?.add(it.data)
                        find.subCommentCount = find.subCommentCount + 1
                        find.isShowingMore = true
                        commentAdapter.updateItem(find)
                    }
                }
                recyclerViewComment.smoothScrollToPosition(
                    commentAdapter.items?.size ?: 0
                )
            }
        })

        motorShowViewModel.likeCommentLiveData.observe(this, {
            if (it.data != null)
                if (it.data.parentCommentId.isNullOrEmpty() || it.data.parentCommentId == "0") {
                    commentAdapter.updateItem(it.data)
                } else {
                    val find =
                        commentAdapter.items?.find { comment -> comment.id == it.data.parentCommentId }
                    if (find != null) {
                        val i = commentAdapter.items?.indexOf(find)
                        if (i != null && i > -1) {
                            (recyclerViewComment.findViewHolderForAdapterPosition(i) as FeedCommentAdapter.FeedCommentHolder)
                                .subCommentAdapter.updateItem(it.data)
                        }
                    }
                }
            commentAdapter.updateItem(it.data!!)

        }, { _, responseBody ->
            responseBody?.data?.isLike = responseBody?.data?.isLike?.not()
            if (responseBody?.data != null)
                commentAdapter.updateItem(responseBody.data)
            true
        })

        motorShowViewModel.favoriteCommentLiveData.observe(this, {
        }, { _, responseBody ->
            responseBody?.data?.isFavourite = responseBody?.data?.isFavourite?.not()
            if (responseBody?.data != null)
                commentAdapter.updateItem(responseBody.data)
            true
        })

        feedViewModel.deleteCommentLiveData.observe(this, {
            onRefresh()
//            if (it.data != null) {
//                if (it.data.parentCommentId.isNullOrEmpty() || it.data.parentCommentId == "0") {
//                    commentAdapter.removeItem(it.data)
//                    val i = commentAdapter.items?.indexOf(it.data)
//                    if (i != null && i >= 0)
//                        feed?.commentCount = feed?.commentCount?.minus(1 + commentAdapter.items!![i].subCommentCount)
//                    checkCommentsNumber()
//                } else {
//                    val find =
//                        commentAdapter.items?.find { comment -> comment.id == it.data.parentCommentId }
//                    if (find != null) {
//                        val i = commentAdapter.items?.indexOf(find)
//                        if (i != null && i > -1) {
//                            (recyclerViewFeedDetailsComment.findViewHolderForAdapterPosition(i) as FeedCommentAdapter.FeedCommentHolder)
//                                .subCommentAdapter.removeItem(it.data)
//
//                            feed?.commentCount = feed?.commentCount?.minus(1)
//                        }
//                    }
//                }
//                checkCommentsNumber()
//            }
        })
    }

//    private fun updateFeedDetails() {
//        showLoader()
//        feedViewModel.getFeedDetail(feed!!.id!!)
//    }

    override fun onResume() {
        super.onResume()
        onRefresh()
    }

    private fun setDetails() {

        Log.d("lacalc123131", story?.id.toString())
        Log.d("lacalc123131", story?.description.toString())
        Log.d("lacalc123131", story?.title.toString())

        /*car.coverImage?.let {
            GlideApp.with(this)
                    .load(it)
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                            try {
                                activity?.supportStartPostponedEnterTransition()
                            } catch (e: Exception) {
                            }
                            return false
                        }
                        override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                            if (isFeatured && vibrantColor == 0 && resource != null) {
                                val p: Palette = Palette.from(resource.toBitmap()).generate()
                                vibrantColor = p.getVibrantColor(getColor(R.color.textColor))
                                imageViewBackground.foregroundTintList = ColorStateList.valueOf(vibrantColor)
                            }
                            try {
                                activity?.supportStartPostponedEnterTransition()
                            } catch (e: Exception) {
                            }
                            return false
                        }
                    }).into(imageViewBackground)
        }*/

        toolbarMotorShowDetail.title = if (story != null) getString(R.string.ORIGINALS)
//        else getString(R.string.auto_partners)
        else ""

        blockFeatured.visibility = if (story != null) View.VISIBLE else View.GONE
        if (story != null) {
            imageViewBackground.visibility = View.VISIBLE
            // imageViewCar.visibility = View.INVISIBLE

            detailScrollView.setOnScrollChangeListener { _: NestedScrollView?, _: Int, scrollY: Int, _: Int, _: Int ->

                if (measuredWidth == 0)
                    measuredWidth = detailScrollView.measuredWidth

                val v = (scrollY.toFloat() / measuredWidth.toFloat())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    changeColor(v)
                }
            }

        } else {
            imageViewBackground.visibility = View.GONE
            //  imageViewCar.visibility = View.VISIBLE
        }

//        val media = if (story != null) story?.media else car.media
        if (story != null) {
            val media = car.media
            val mediaStory = story?.media

            if (!media.isNullOrEmpty()) {
                adapter.media.clear()
                adapter.media.addAll(mediaStory!!)
                adapter.media.addAll(media)
                adapter.notifyDataSetChanged()
            } else {
                viewPagerMedia.visibility = View.GONE
            }
        } else {
            val media = car.media

            if (!media.isNullOrEmpty()) {
                adapter.media.clear()
                adapter.media.addAll(media)
                adapter.notifyDataSetChanged()
            } else {
                viewPagerMedia.visibility = View.GONE
            }
        }

//        val media = car.media
//        val mediaStory = story?.media
//
//        if (!media.isNullOrEmpty()) {
//            adapter.media.clear()
//            adapter.media.addAll(mediaStory!!)
//            adapter.media.addAll(media)
//            adapter.notifyDataSetChanged()
//        } else {
//            viewPagerMedia.visibility = View.GONE
//        }

        textViewCarName.text = story?.title ?: car.headline

        textViewAboutCar.text = story?.description ?: car.description

        textViewAboutCar.setOnMentionClickListener { _, mention ->
            loadMentionUserId(mention, profileViewModel) {
                if (it != null && session.userId != it.id) {
                    if (!it.isBusiness) {
                        val action = MotorShowDetailFragmentDirections.actionGlobalToProfile()
                        action.user = it
                        findNavController().navigate(action)
                    } else {
                        val action =
                            MotorShowDetailFragmentDirections.actionGlobalToProfileBusiness()
                        action.user = it
                        findNavController().navigate(action)
                    }
                }
            }
        }

        textViewAboutCar.setOnHashtagClickListener { _, hashtag ->
            val action =
                MotorShowDetailFragmentDirections.actionGlobalToHashtagList(HashTag(tag = hashtag))
            findNavController().navigate(action)
        }

        textViewAboutCar.visibility =
            if (textViewAboutCar.text.isEmpty()) View.GONE else View.VISIBLE

        Log.d("KDMKDKMKM", "likeCount " + car.likeCount.toString())
        Log.d("KDMKDKMKM", "car " + car.toString())

        textViewLike.text = if (car.likeCount!! < 999) car.likeCount.default(0)
            .toString() else getString(R.string.counterOverValue)

        textViewLike.tag = car.isLike ?: false
        imageButtonBookmark.tag = car.isFavorite ?: false

        changeLikeStatus(textViewLike.tag as Boolean)
        changeBookmarkStatus(imageButtonBookmark.tag as Boolean)

        profileImage.loadProfilePicture(car.profileImage)
        textViewName.text = car.username

        checkCommentsNumber()

        checkOwnerBlock()

        if (car.insertdate?.time != null)
            textViewDuration.text = DateUtils.getRelativeTimeSpanString(
                car.insertdate?.time!!,
                Date().time,
                DateUtils.MINUTE_IN_MILLIS
            )

//        buttonMessage.visibility = if (isMe) View.INVISIBLE else View.VISIBLE

        if (car.tagsDetails?.isNotEmpty() == true)
            tagLayout.setTagList(car.tagsDetails!!)
        else if (!car.tags.isNullOrBlank()) {
            tagLayout.setTagList(
                car.tags?.split(",")?.map { s -> HashTag(tag = s) } as ArrayList<HashTag>)
        } else {
            tagLayout.visibility = View.GONE
        }

        textViewManufactured.text = car.year ?: ""
        textViewHorsePower.text = getString(R.string.pattern_engine_power, car.horsePower)
        textViewCapacity.text = getString(R.string.pattern_engine_capacity, car.engineCapacity)

        textViewCarStatus.text = getString(R.string.pattern_car_type, car.type)

        val specifications = ArrayList<Pair<String, String>>()
        specifications.add(Pair(getString(R.string.car_info_make), car.brand ?: ""))
        specifications.add(Pair(getString(R.string.car_info_model), car.model ?: ""))

        if (!car.modelVariant.isNullOrBlank())
            specifications.add(
                Pair(
                    getString(R.string.car_info_model_variant),
                    car.modelVariant ?: ""
                )
            )
        if (!car.generation.isNullOrBlank())
            specifications.add(Pair(getString(R.string.car_info_generation), car.generation ?: ""))
        if (!car.engine.isNullOrBlank())
            specifications.add(Pair(getString(R.string.car_info_engine), car.engine ?: ""))
        if (!car.body.isNullOrBlank())
            specifications.add(Pair(getString(R.string.car_info_body), car.body ?: ""))
        if (!car.color.isNullOrBlank())
            specifications.add(Pair(getString(R.string.car_info_color), car.color ?: ""))

        if (!car.purchaseDate.isNullOrBlank())
            specifications.add(
                Pair(
                    getString(R.string.car_info_purchase_date),
                    car.purchaseDate ?: ""
                )
            )

        if (!car.soldDate.isNullOrBlank())
            specifications.add(Pair(getString(R.string.car_info_sold_date), car.soldDate ?: ""))

        carSpecificationDetailAdapter.items = specifications
        modificationAdapter.items = car.modifications

        if (modificationAdapter.items?.isEmpty() == true) {
            textViewLabelModification.visibility = View.GONE
            recycleViewModification.visibility = View.GONE
        } else {
            textViewLabelModification.visibility = View.VISIBLE
            recycleViewModification.visibility = View.VISIBLE
        }

        fillMoments()

        motorShowViewModel.getComments(car)

//        if (car.sponsors?.isNotEmpty() == true || isMe) {
        blockSponsors.visibility = View.VISIBLE
        sponsorsAdapter.items = car.sponsors
//        } else blockSponsors.visibility = View.GONE
    }

    private fun checkOwnerBlock() {
        if (!isMe) {
            blockOwner.visibility = View.VISIBLE

            if (car.isFollowing == true) {
                buttonFollow.visibility = View.GONE
            } else {
                buttonFollow.visibility = View.VISIBLE
                buttonFollow.isEnabled = true
                buttonFollow.text = getString(R.string.follow)
            }
        } else {
            blockOwner.visibility = View.GONE
        }
    }

    private fun fillCommentsCount() {
        textViewCommentCount.text = if (car.commentCount!! < 999)
            getString(
                R.string.pattern_comments,
                car.commentCount.default(0).toString()
            ) else getString(R.string.counterOverValue)
    }

    private fun checkCommentsNumber() {
        fillCommentsCount()

        val visibility = if (car.commentCount == 0) View.GONE else View.VISIBLE
        recyclerViewComment.visibility = visibility
        textViewCommentCount.visibility = visibility
        buttonComments.visibility = visibility
    }

    private fun fillMoments() {
        if (car.moments?.isNotEmpty() == true) {
            recycleViewMoments.visibility = View.VISIBLE
            val maxIndex = if (car.moments?.size!! >= 5) 5 else car.moments?.size!!

            momentsAdapterPreview.items = car.moments?.subList(0, maxIndex)
//            if (car.moments?.size!! > 5) {
//                buttonMoments.visibility = View.VISIBLE
//                buttonAddMoment.visibility = View.GONE
//            } else {
//                buttonMoments.visibility = View.GONE
//                buttonAddMoment.visibility = if (isMe) View.VISIBLE else View.GONE
//            }
        } else {
//            textViewLabelMoments.visibility = View.INVISIBLE
            buttonMoments.visibility = View.GONE
            buttonAddMoment.visibility = if (isMe) View.VISIBLE else View.GONE
        }
    }

    private fun changeLikeStatus(status: Boolean) {
        textViewLike.iconTint = ColorStateList.valueOf(
            getColor(if (status) R.color.colorAccent else R.color.iconColor)
        )

        textViewLike.icon =
            getDrawable(if (status) R.drawable.ic_heart_a else R.drawable.ic_like)
    }

    private fun changeBookmarkStatus(status: Boolean) {
        imageButtonBookmark.icon =
            getDrawable(if (status) R.drawable.ic_star_white else R.drawable.ic_star_border_white)
        imageButtonBookmark.iconTint = ColorStateList.valueOf(
            getColor(if (status) R.color.colorAccent else R.color.iconColor)
        )
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun changeColor(fraction: Float) {

        val evaluate = evaluate(fraction, vibrantColor, getColor(R.color.imagePlaceholder))
        imageViewBackground.foregroundTintList = ColorStateList.valueOf(evaluate as Int)

    }

    private fun evaluate(fraction: Float, startValue: Any, endValue: Any): Any {
        val startInt = startValue as Int
        val startA = (startInt shr 24 and 0xff) / 255.0f
        var startR = (startInt shr 16 and 0xff) / 255.0f
        var startG = (startInt shr 8 and 0xff) / 255.0f
        var startB = (startInt and 0xff) / 255.0f

        val endInt = endValue as Int
        val endA = (endInt shr 24 and 0xff) / 255.0f
        var endR = (endInt shr 16 and 0xff) / 255.0f
        var endG = (endInt shr 8 and 0xff) / 255.0f
        var endB = (endInt and 0xff) / 255.0f

        // convert from sRGB to linear
        startR = startR.toDouble().pow(2.2).toFloat()
        startG = startG.toDouble().pow(2.2).toFloat()
        startB = startB.toDouble().pow(2.2).toFloat()

        endR = endR.toDouble().pow(2.2).toFloat()
        endG = endG.toDouble().pow(2.2).toFloat()
        endB = endB.toDouble().pow(2.2).toFloat()

        // compute the interpolated color in linear space
        var a = startA + fraction * (endA - startA)
        var r = startR + fraction * (endR - startR)
        var g = startG + fraction * (endG - startG)
        var b = startB + fraction * (endB - startB)

        // convert back to sRGB in the [0..255] range
        a *= 255.0f
        r = r.toDouble().pow(1.0 / 2.2).toFloat() * 255.0f
        g = g.toDouble().pow(1.0 / 2.2).toFloat() * 255.0f
        b = b.toDouble().pow(1.0 / 2.2).toFloat() * 255.0f

        return Math.round(a) shl 24 or (Math.round(r) shl 16) or (Math.round(g) shl 8) or Math.round(
            b
        )
    }

    private fun onClick(view: View) {
        when (view) {
            profileImage, textViewName -> {

                Log.d("MYTGAGGA", car.userId.toString())

                if (session.userId != car.userId) {
                    if (car.role == 1) {
                        val action =
                            MotorShowDetailFragmentDirections.actionGlobalToProfileBusiness()
                        action.user = User(id = car.userId, profileImage = car.profileImage)
                        findNavController().navigate(action)
                    } else {
                        val action = MotorShowDetailFragmentDirections.actionGlobalToProfile()
                        action.user = User(id = car.userId, profileImage = car.profileImage)
                        findNavController().navigate(action)
                    }
                }
            }
            imageButtonBookmark -> {
                imageButtonBookmark.tag = (imageButtonBookmark.tag as Boolean).not()
                changeBookmarkStatus(imageButtonBookmark.tag as Boolean)
                motorShowViewModel.favorite(car)
            }
            textViewLike -> {
                textViewLike.tag = (textViewLike.tag as Boolean).not()
                changeLikeStatus(textViewLike.tag as Boolean)
                motorShowViewModel.likeUnlike(car)
            }
            buttonComments, clAddComment -> {
                car.requestType = "motorshow"
                val action =
                    MotorShowDetailFragmentDirections.actionGlobalToComments(car as HasComment)
                findNavController().navigate(action)
            }
            buttonShare -> {
                shareManage.shareCarDetail(car)
            }
            buttonFollow -> {
                profileViewModel.follow(!(car.isFollowing ?: false), car.userId!!)
            }
            buttonMoments -> {
                val action = MotorShowDetailFragmentDirections.actionVehicleDetailsToDestMoments(
                    car.id!!,
                    car.userId == session.userId,
                    car.moments?.toTypedArray() as Array<out Moment>
                )
                findNavController().navigate(action)
            }
            buttonAddMoment -> {
                navigator.loadActivity(PlainActivity::class.java, AddMomentsFragment::class.java)
                    .forResult(Common.RequestCode.ADD_MOMENT)
                    .addBundle(
                        bundleOf(
                            "vehicleId" to car.id
                        )
                    )
                    .start(this)
            }
            buttonSponsors -> {
                if (isMe) {
                    val action =
                        MotorShowDetailFragmentDirections.actionDestVehicleToDestSponsorship(
                            User(id = car.userId, role = 0), car
                        )
                    findNavController().navigate(action)
                } else {
                    val action =
                        MotorShowDetailFragmentDirections.actionDestVehicleToDestSponsorshipOther(
                            SponsorshipListFragment.TYPE_CONFIRMED,
                            SponsorshipListFragment.SECTION_VEHICLE
                        )
                    action.vehicle = car
                    action.sponsorship = car.sponsors!!.toTypedArray()
                    action.type = SponsorshipListFragment.TYPE_CONFIRMED
                    action.section = SponsorshipListFragment.SECTION_BUSINESS
                    action.toolbar = true
                    findNavController().navigate(action)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                Common.RequestCode.CREATE_VEHICLE -> {
                    car = data!!.getParcelableExtra(Vehicle.KEY)!!
                    setDetails()
                }
                Common.RequestCode.ADD_MOMENT -> {
                    val moment = data?.getParcelableArrayListExtra<Moment>(Moment.KEY)
                    if (moment != null) {
                        car.moments = moment
                        fillMoments()
                    }
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    var menuEdit: MenuItem? = null

    override fun createOptionsMenu() {
//        toolbarMotorShowDetail.inflateMenu(if (isMe) R.menu.motorshow_option_me_menu else R.menu.motorshow_option_menu)
        if (isMe) {
            toolbarMotorShowDetail.inflateMenu(R.menu.motorshow_option_me_menu)
            menuEdit = /*if (reviewType == "motorshow")
                menu.add(R.string.add)
            else*/
                toolbarMotorShowDetail.menu.add(R.string.edit)
            menuEdit?.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            menuEdit?.icon = getDrawable(R.drawable.ic_edit)
        }
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when {
            menuEdit == item -> {
                navigator.loadActivity(PlainActivity::class.java, CreateVehicleFragment::class.java)
                    .addBundle(bundleOf(Vehicle.KEY to car))
                    .forResult(Common.RequestCode.CREATE_VEHICLE)
                    .start(this)
                return true
            }
//            item?.itemId == R.id.menuReportForFake -> {
//                showAlert(
//                    getString(R.string.report),
//                    getString(R.string.message_report_vehicle_as_fake),
//                    R.string.yes,
//                    { motorShowViewModel.report(car.id!!, Reporter.FAKE_VEHICLE) },
//                    R.string.no,
//                    {})
//                return true
//            }
            item == menuEdit -> {
//                if (reviewType == "motorshow") {
//                    val momentsMedia = car.moments?.flatMap { moment ->
//                        moment.media ?: arrayListOf()
//                    }
//
//                    val mediaToUpload = ArrayList<Media>()
//
//                    if (car.media != null)
//                        mediaToUpload.addAll(car.media!!)
//
//                    if (momentsMedia != null)
//                        mediaToUpload.addAll(momentsMedia)
//
//                    //Remove all uploaded media
//                    mediaToUpload.removeAll { media -> URLUtil.isNetworkUrl(media.path) }
//
//                    showLoader()
//                    if (mediaToUpload.isEmpty()) {
//                        onUploadComplete()
//                    } else uploadImages(mediaToUpload)

//                } else
//                    navigator.goBack()
                return true
            }

            item?.itemId == R.id.menuDeleteVehicle -> {
                showAlert(
                    R.string.title_delete_vehicle,
                    R.string.message_confirmation_delete_vehicle,
                    R.string.yes,
                    {
                        showLoader()
                        profileViewModel.deleteVehicle(car)
                    },
                    R.string.no,
                    {})
                return true
            }
//            item?.itemId == R.id.menuNoLongerOwned -> {
//
//                showAlert(R.string.title_vehicle_no_longer_owned
//                    , R.string.message_confirmation_vehicle_no_longer_owned
//                    , R.string.yes
//                    , { motorShowViewModel.moveToPreviousGarage(car) }
//                    , R.string.no, {})
//                return true
//            }
            item?.itemId == R.id.menuEnterToMotorShow -> {
                motorShowViewModel.moveToMotorShow(car)
                return true
            }
        }
        return false
    }

    override fun onRefresh() {
//        if (story != null) {
//            showLoader()
//            motorShowViewModel.getStoryDetail(story?.id!!)
//        } else if (car.id != null) {
        showLoader()
        motorShowViewModel.getVehicleDetail(car.id!!)
//        }
    }

    override fun onShowMoreLess(comment: Comment) {
        commentAdapter.updateItem(comment)
    }

    override fun onReply(comment: Comment, isSub: Boolean) {
        val action = MotorShowDetailFragmentDirections.actionGlobalToComments(car as HasComment)
        car.requestType = "motorshow"
        if (isSub) {
            var position = -1
            if (commentAdapter.items != null)
                commentAdapter.items!!.forEach {
                    if (it.subComments != null) {
                        val pos = it.subComments!!.indexOf(comment)
                        if (pos != -1) {
                            position = commentAdapter.items!!.indexOf(it)
                            return@forEach
                        }
                    }
                }
            action.position = position
            findNavController().navigate(action)
        } else {
            if (commentAdapter.items != null)
                action.position = commentAdapter.items!!.indexOf(comment)
        }
        action.activeComment = comment
        action.isSub = isSub
        findNavController().navigate(action)
    }

    override fun onLike(current: Comment) {
        current.isLike = current.isLike?.not()

        if (current.isLike!!)
            current.likeCount = current.likeCount + 1
        else current.likeCount = current.likeCount - 1

        motorShowViewModel.likeComment(current, car.requestType)

    }

    override fun onHashTag(current: String) {
        val action =
            MotorShowDetailFragmentDirections.actionGlobalToHashtagList(HashTag(tag = current))
        findNavController().navigate(action)
    }

    override fun onOption(current: Comment, view: View) {
        val popupMenu = PopupMenu(requireContext(), view)

        popupMenu.inflate(if (session.userId == current.userId) R.menu.feed_details_options_del else R.menu.comment_details_more_report)
        val deleteComment = popupMenu.menu.findItem(R.id.menuDeleteComment)
        if (deleteComment != null)
            deleteComment.title = getString(R.string.delete_comment)

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menuDeleteComment -> {
                    showLoader()
                    feedViewModel.deleteComment(current, car.requestType)
                }
                R.id.menuReportComment -> {
                    Reporter(requireContext(), feedViewModel).reportComment(
                        car.id!!, current.id,
                        when (car.requestType) {
                            HasComment.REQUEST_TYPE_FEED -> Reporter.FEED
                            HasComment.REQUEST_TYPE_MOTORSHOW -> Reporter.MOTORSHOW
                            HasComment.REQUEST_TYPE_REVIEW -> Reporter.REVIEW
                            HasComment.REQUEST_TYPE_EVENT -> Reporter.EVENT
                            HasComment.REQUEST_TYPE_GROUP -> Reporter.GROUP
                            else -> Reporter.FEED
                        }
                    )
                }
            }
            true
        }

        popupMenu.show()
    }

    override fun onMentionClick(userName: String) {
        loadMentionUserId(userName, profileViewModel) {
            if (it?.id != session.userId) {
                if (it?.role == 0) {
                    val action = MotorShowDetailFragmentDirections.actionGlobalToProfile()
                    action.user = it
                    findNavController().navigate(action)
                } else {
                    val action = MotorShowDetailFragmentDirections.actionGlobalToProfileBusiness()
                    action.user = it
                    findNavController().navigate(action)
                }
            }
        }
    }

    override fun onUserClick(user: User) {
        if (session.userId != user.id) {
            if (user.role == 0) {
                val action = MotorShowDetailFragmentDirections.actionGlobalToProfile()
                action.user = User(id = user.id)
                findNavController().navigate(action)
            } else {
                val action = MotorShowDetailFragmentDirections.actionGlobalToProfileBusiness()
                action.user = User(id = user.id)
                findNavController().navigate(action)
            }
        }
    }
}