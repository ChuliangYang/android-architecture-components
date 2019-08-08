/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.example.paging.pagingwithnetwork.reddit.repository.inDb

import androidx.lifecycle.MutableLiveData
import androidx.paging.PagedList
import androidx.paging.PagingRequestHelper
import com.android.example.paging.pagingwithnetwork.reddit.api.RedditApi
import com.android.example.paging.pagingwithnetwork.reddit.db.RedditDb
import com.android.example.paging.pagingwithnetwork.reddit.repository.NetworkState
import com.android.example.paging.pagingwithnetwork.reddit.vo.RedditPost
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.Executors

/**
 * This boundary callback gets notified when user reaches to the edges of the list such that the
 * database cannot provide any more data.
 * <p>
 * The boundary callback might be called multiple times for the same direction so it does its own
 * rate limiting using the PagingRequestHelper class.
 */
class mSubredditBoundaryCallback(val retrofitApi: RedditApi, val subreddit: String, val limit: Int, val redditDb: RedditDb) : PagedList.BoundaryCallback<RedditPost>() {

    val pageRequestHelper = PagingRequestHelper(Executors.newSingleThreadExecutor())
    val netWorkStatus: MutableLiveData<NetworkState> = MutableLiveData()

    init {
        monitorRequestStatus()
    }

    private fun monitorRequestStatus() {
        pageRequestHelper.addListener {
            when {
                it.hasRunning() -> netWorkStatus.postValue(NetworkState.LOADING)
                it.hasError() -> netWorkStatus.postValue(NetworkState.error(it.getErrorMessage()))
                else -> netWorkStatus.postValue(NetworkState.LOADED)
            }
        }
    }

    override fun onZeroItemsLoaded() {
        pageRequestHelper.runIfNotRunning(PagingRequestHelper.RequestType.INITIAL) {
            retrofitApi.getTop(subreddit, limit).enqueue(insertIntoDBTask(it))
        }
    }

    override fun onItemAtEndLoaded(itemAtEnd: RedditPost) {
        pageRequestHelper.runIfNotRunning(PagingRequestHelper.RequestType.AFTER) {
            retrofitApi.getTopAfter(subreddit, itemAtEnd.name, limit).enqueue(insertIntoDBTask(it))
        }
    }

    private fun insertIntoDBTask(it: PagingRequestHelper.Request.Callback): Callback<RedditApi.ListingResponse?> {
        return object : Callback<RedditApi.ListingResponse?> {
            override fun onFailure(call: Call<RedditApi.ListingResponse?>, t: Throwable) {
                it.recordFailure(t)
            }

            override fun onResponse(call: Call<RedditApi.ListingResponse?>, response: Response<RedditApi.ListingResponse?>) {
                response.body()?.data?.children?.let { response ->
                    val postDao = redditDb.posts()
                    postDao.insert(response.map { child ->
                        child.data
                    })
                }
                it.recordSuccess()
            }
        }
    }

}
