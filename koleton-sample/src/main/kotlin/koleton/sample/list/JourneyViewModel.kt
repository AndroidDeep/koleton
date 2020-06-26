package koleton.sample.list

import androidx.lifecycle.*
import androidx.lifecycle.Transformations.map
import koleton.sample.list.repository.JourneyRepository
import java.util.concurrent.Executors

class JourneyViewModel(
    private val repository: JourneyRepository
): ViewModel() {

    private val liveData = MutableLiveData<Long>()

    private val repoResult = map(liveData) {
        repository.getJourneyList(it)
    }

    val posts = repoResult.switchMap { it.pagedList }
    val state = repoResult.switchMap { it.networkState }
    val refreshState = repoResult.switchMap { it.refreshState }

    fun refresh() {
        repoResult.value?.refresh?.invoke()
    }

    fun load(delay: Long) {
        liveData.value = delay
    }

    fun retry() {
        val listing = repoResult.value
        listing?.retry?.invoke()
    }
}