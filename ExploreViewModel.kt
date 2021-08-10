package com.example.app.ui.explore

import androidx.lifecycle.MutableLiveData
import com.example.app.data.model.*
import com.example.app.data.repository.UserRepository
import com.example.app.ui.base.APILiveData
import com.example.app.ui.base.BaseViewModel
import javax.inject.Inject

class ExploreViewModel @Inject constructor(val userRepository: UserRepository) : BaseViewModel() {

    val carsLiveData: APILiveData<Pagination<Vehicle>> by lazy {
        APILiveData<Pagination<Vehicle>>()
    }
    val peopleLiveData: APILiveData<Pagination<User>> by lazy {
        APILiveData<Pagination<User>>()
    }
    val blockedPeopleLiveData: APILiveData<Pagination<User>> by lazy {
        APILiveData<Pagination<User>>()
    }
    val businessLiveData: APILiveData<Pagination<User>> by lazy {
        APILiveData<Pagination<User>>()
    }
    val groupsLiveData: APILiveData<Pagination<Group>> by lazy {
        APILiveData<Pagination<Group>>()
    }
    val blockedGroupsLiveData: APILiveData<Pagination<Group>> by lazy {
        APILiveData<Pagination<Group>>()
    }
    val hiddenGroupsLiveData: APILiveData<Pagination<Group>> by lazy {
        APILiveData<Pagination<Group>>()
    }
    val eventsLiveData: APILiveData<Pagination<Event>> by lazy {
        APILiveData<Pagination<Event>>()
    }

    val businessReviewLiveData: APILiveData<Pagination<BusinessReview>> by lazy {
        APILiveData<Pagination<BusinessReview>>()
    }
    val carReviewLiveData: APILiveData<Pagination<CarReview>> by lazy {
        APILiveData<Pagination<CarReview>>()
    }

    val feedLiveData: APILiveData<Pagination<Feed>> by lazy {
        APILiveData<Pagination<Feed>>()
    }
    val storyLiveData: APILiveData<Pagination<Story>> by lazy {
        APILiveData<Pagination<Story>>()
    }
    val hiddenFeedLiveData: APILiveData<Pagination<Feed>> by lazy {
        APILiveData<Pagination<Feed>>()
    }

    val searchLiveData: MutableLiveData<String> = MutableLiveData()

    fun loadCars(bodyBuilder: BodyBuilder) {
        feedRepository.exploreCars(bodyBuilder)
                .subscribe(withLiveData(carsLiveData))
    }

    fun loadPeople(bodyBuilder: BodyBuilder) {
        feedRepository.explorePeople(bodyBuilder)
                .subscribe(withLiveData(peopleLiveData))
    }

    fun loadBlockedPeople(bodyBuilder: BodyBuilder) {
        userRepository.getBlockedUsersList(bodyBuilder)
                .subscribe(withLiveData(blockedPeopleLiveData))
    }

    fun loadBusiness(bodyBuilder: BodyBuilder) {
        feedRepository.explorePeople(bodyBuilder)
                .subscribe(withLiveData(businessLiveData))
    }

    fun loadGroups(bodyBuilder: BodyBuilder) {
        feedRepository.exploreGroups(bodyBuilder)
                .subscribe(withLiveData(groupsLiveData))
    }

    fun loadBlockedGroups(bodyBuilder: BodyBuilder) {
        userRepository.getBlockedGroupsList(bodyBuilder)
                .subscribe(withLiveData(blockedGroupsLiveData))
    }

    fun loadHiddenGroups(bodyBuilder: BodyBuilder) {
        userRepository.getHiddenGroupList(bodyBuilder)
                .subscribe(withLiveData(hiddenGroupsLiveData))
    }

    fun loadEvent(bodyBuilder: BodyBuilder) {
        feedRepository.exploreEvent(bodyBuilder)
                .subscribe(withLiveData(eventsLiveData))
    }

    fun loadBusinessReview(bodyBuilder: BodyBuilder) {
        feedRepository.exploreBusinessReview(bodyBuilder)
                .subscribe(withLiveData(businessReviewLiveData))
    }

    fun loadCarReview(bodyBuilder: BodyBuilder) {
        feedRepository.exploreCarReview(bodyBuilder)
                .subscribe(withLiveData(carReviewLiveData))
    }

    fun loadFeed(bodyBuilder: BodyBuilder) {
        feedRepository.exploreFeed(bodyBuilder)
                .subscribe(withLiveData(feedLiveData))
    }

    fun loadStory(bodyBuilder: BodyBuilder) {
        feedRepository.exploreStory(bodyBuilder)
                .subscribe(withLiveData(storyLiveData))
    }

    fun loadHiddenFeed(bodyBuilder: BodyBuilder) {
        userRepository.getHiddenPostList(bodyBuilder)
                .subscribe(withLiveData(hiddenFeedLiveData))
    }


}