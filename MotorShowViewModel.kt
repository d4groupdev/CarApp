package com.example.app.ui.motorshow

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.app.data.model.*
import com.example.app.data.repository.MotorShowRepository
import com.example.app.ui.base.APILiveData
import com.example.app.ui.base.BaseViewModel
import javax.inject.Inject

class MotorShowViewModel @Inject constructor(private val motorShowRepository: MotorShowRepository) :
    BaseViewModel() {


    private val _cars: MutableLiveData<List<Vehicle>> = MutableLiveData()
    val cars: LiveData<List<Vehicle>>
        get() = _cars

    val myShowListLiveData = APILiveData<Pagination<Vehicle>>()
    val trandingListLiveData = APILiveData<Pagination<Vehicle>>()
    val allMotorShowLiveData = APILiveData<Pagination<Vehicle>>()
    val likeUnlikeLiveData = APILiveData<Any>()
    val favoriteLiveData = APILiveData<HasComment>()
    val makeOfferLiveData = APILiveData<Any>()
    val makeOfferActionLiveData = APILiveData<Alerts>()
    val commentsListLiveData = APILiveData<ArrayList<Comment>>()
    val commentLiveData = APILiveData<Comment>()
    val likeCommentLiveData = APILiveData<Comment>()
    val favoriteCommentLiveData = APILiveData<Comment>()

    val vehicleDetailLiveData = APILiveData<Vehicle>()
    val storyDetailLiveData = APILiveData<Story>()

    val moveVehicleLiveData = APILiveData<Any>()
    val addRemoveMotorShowLiveData = APILiveData<Any>()

    val userMentionListLiveData = APILiveData<ArrayList<User>>()
    val offerDetailsLiveData = APILiveData<Offer>()

    val vehicleQRLiveData = APILiveData<Vehicle>()
    val vehicleQRDesableLiveData = APILiveData<Any>()

    val inviteSponsorLiveData = APILiveData<Any>()

    fun loadMyShow(page: Int, searchText: String? = null, feedFilter: FeedFilter?) {
        motorShowRepository.getMotorShowList(
            BodyBuilder(
                type = "foryou", page = page
                , searchText = searchText, filter = feedFilter
            )
        ).subscribe(withLiveData(myShowListLiveData))
    }

    fun loadTrandingShow(page: Int, searchText: String? = null, feedFilter: FeedFilter?) {
        motorShowRepository.getMotorShowList(
            BodyBuilder(
                type = "trending", page = page,
                searchText = searchText, filter = feedFilter
            )
        ).subscribe(withLiveData(trandingListLiveData))
    }

    fun loadAllShow(page: Int, searchText: String? = null, feedFilter: FeedFilter?) {
        motorShowRepository.getMotorShowList(
            BodyBuilder(
                type = "all", page = page,
                searchText = searchText, filter = feedFilter
            )
        ).subscribe(withLiveData(allMotorShowLiveData))
    }

    fun likeUnlike(vehicle: HasComment) {
        feedRepository.likeUnlike(BodyBuilder(globalId = vehicle.id, type = vehicle.requestType))
            .subscribe(withLiveData(likeUnlikeLiveData))
    }

    fun likeUnlikeStory(vehicle: HasComment) {
        feedRepository.likeUnlike(BodyBuilder(globalId = vehicle.id, type = vehicle.requestType))
            .subscribe(withLiveData(likeUnlikeLiveData))
    }

    fun favorite(vehicle: HasComment) {
        feedRepository.makeFavorite(BodyBuilder(globalId = vehicle.id, type = vehicle.requestType))
            .map(responseWith(vehicle))
            .subscribe(withLiveData(favoriteLiveData))
    }


    fun makeOffer(bodyBuilder: BodyBuilder) {
        motorShowRepository.makeAnOffer(bodyBuilder).subscribe(withLiveData(makeOfferLiveData))
    }

    fun makeOfferAction(alerts: Alerts, bodyBuilder: BodyBuilder) {
        motorShowRepository.makeOfferAction(bodyBuilder)
            .map(responseWith(alerts))
            .subscribe(withLiveData(makeOfferActionLiveData))
    }

    fun getComments(hasComment: HasComment) {
        feedRepository.getComments(
            BodyBuilder(
                globalId = hasComment.id,
                type = hasComment.requestType
            )
        )
            .subscribe(withLiveData(commentsListLiveData))
    }

    fun makeComment(bodyBuilder: BodyBuilder) {
        feedRepository.makeComment(bodyBuilder).subscribe(withLiveData(commentLiveData))
    }

    fun likeComment(comment: Comment, requestType: String?) {
        motorShowRepository.likeUnlikeComment(
            BodyBuilder(
                commentId = comment.id,
                type = requestType
            )
        )
            .map { t ->
                DataWrapper(
                    ResponseBody(
                        t.responseBody?.responseCode
                            ?: 0, t.responseBody?.message
                            ?: "", comment
                    ), null
                )
            }
            .subscribe(withLiveData(likeCommentLiveData))
    }

    fun favoriteComment(comment: Comment, requestType: String?) {
        motorShowRepository.makeFavoriteComment(
            BodyBuilder(
                commentId = comment.id,
                type = requestType
            )
        )
            .map { t ->
                DataWrapper(
                    ResponseBody(
                        t.responseBody?.responseCode
                            ?: 0, t.responseBody?.message
                            ?: "", comment
                    ), null
                )
            }
            .subscribe(withLiveData(favoriteCommentLiveData))
    }

    fun getVehicleDetail(vehicleId: String) {
        motorShowRepository.getVehicleDetail(BodyBuilder(vehicleId = vehicleId))
            .subscribe(withLiveData(vehicleDetailLiveData))
    }

    fun getStoryDetail(storyID: String) {
        feedRepository.storyDetails(BodyBuilder(storyId = storyID))
            .subscribe(withLiveData(storyDetailLiveData))
    }

    fun moveToPreviousGarage(car: Vehicle) {
        motorShowRepository.moveToPreviousGarage(BodyBuilder(vehicleId = car.id))
            .subscribe(withLiveData(moveVehicleLiveData))
    }

    fun moveToMotorShow(car: Vehicle) {
        motorShowRepository.moveRemoveToMotorShow(
            BodyBuilder(
                vehicleId = car.id,
                action = if (car.isMotorshow == true) 0 else 1
            )
        )
            .subscribe(withLiveData(addRemoveMotorShowLiveData))
    }

    fun moveToMotorShow(car: ArrayList<Vehicle>) {
        motorShowRepository.moveRemoveToMotorShow(
            BodyBuilder(
                vehicleId = car.joinToString(",") { vehicle -> vehicle.id!! },
                action = 1
            )
        )
            .subscribe(withLiveData(addRemoveMotorShowLiveData))
    }

    fun loadMentionList(username: String) {
        feedRepository.getMentionList(BodyBuilder(userName = username))
            .subscribe(withLiveData(userMentionListLiveData))
    }

    fun getOfferDetail(otherId: String?) {
        motorShowRepository.getOfferDetail(BodyBuilder(otherId = otherId))
            .subscribe(withLiveData(offerDetailsLiveData))
    }

    fun enableQRCode(vehicleId: String, url: String) {
        motorShowRepository.enableQRCode(BodyBuilder(vehicleId = vehicleId, qrData = url))
            .subscribe(withLiveData(vehicleQRLiveData))
    }

    fun desableQRCode(vehicleId: String) {
        motorShowRepository.disableQRCode(BodyBuilder(vehicleId = vehicleId))
            .subscribe(withLiveData(vehicleQRDesableLiveData))
    }

    fun inviteSponsor(sponsor: Sponsorship) {
        (
            if (sponsor.id == null)
                motorShowRepository.inviteSponsor(sponsor)
            else
                motorShowRepository.updateSponsor(sponsor)
                )
        .subscribe(withLiveData(inviteSponsorLiveData))
    }
}