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

import androidx.paging.LivePagedListBuilder
import androidx.annotation.MainThread
import androidx.lifecycle.*
import androidx.paging.PagedList
import com.android.example.paging.pagingwithnetwork.reddit.api.RedditApi
import com.android.example.paging.pagingwithnetwork.reddit.db.RedditDb
import com.android.example.paging.pagingwithnetwork.reddit.repository.NetworkState
import com.android.example.paging.pagingwithnetwork.reddit.repository.mRedditPostRepository
import com.android.example.paging.pagingwithnetwork.reddit.vo.RedditPost
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.Executors

/**
 * Repository implementation that uses a database PagedList + a boundary callback to return a
 * listing that loads in pages.
 */
class mDbRedditPostRepository(
        val db: RedditDb,
        private val redditApi: RedditApi,
        private val networkPageSize: Int = DEFAULT_NETWORK_PAGE_SIZE) : mRedditPostRepository {
    companion object {
        private const val DEFAULT_NETWORK_PAGE_SIZE = 10
    }
    private val refreshStateLiveData=MutableLiveData<MutableLiveData<NetworkState>>()
    private val postPageListLiveData=MutableLiveData<LiveData<PagedList<RedditPost>>>()
    private val refreshStateMutable=MutableLiveData<NetworkState>()

    override var loadingState=Transformations.switchMap(refreshStateLiveData){
         it
    }

    override var postPageList=Transformations.switchMap(postPageListLiveData){
        it
    }

    override var refreshState=Transformations.map(refreshStateMutable){
        it
    }

    /**
     * When refresh is called, we simply run a fresh network request and when it arrives, clear
     * the database table and insert all new items in a transaction.
     * <p>
     * Since the PagedList already uses a database bound data source, it will automatically be
     * updated after the database transaction is finished.
     */
    @MainThread
    private fun refresh(subredditName: String){
        refreshStateMutable.value=NetworkState.LOADING
        redditApi.getTop(subredditName,networkPageSize).enqueue(object : Callback<RedditApi.ListingResponse?> {
            override fun onFailure(call: Call<RedditApi.ListingResponse?>, t: Throwable) {
                refreshStateMutable.value=NetworkState.error(t.message)
            }

            override fun onResponse(call: Call<RedditApi.ListingResponse?>, response: Response<RedditApi.ListingResponse?>) {
                response.body()?.data?.children?.let {responseList->
                    val posts=responseList.map {
                        it.data
                    }

                    Executors.newSingleThreadExecutor().execute {
                        db.runInTransaction {
                            db.posts().run {
                                deleteBySubreddit(subredditName)
                                insert(posts)
                            }
                        }
                    }

                    refreshStateMutable.value=NetworkState.LOADED
                }
            }
        })
    }

    /**
     * Returns a Listing for the given subreddit.
     */
    @MainThread
     override fun postsOfSubreddit(subReddit: String, pageSize: Int) {
        val mSubredditBoundaryCallback=mSubredditBoundaryCallback(redditApi,subReddit,networkPageSize,db)
        val dataSourceFactory=db.posts().postsBySubreddit(subReddit)
        val pageListLiveData=LivePagedListBuilder(dataSourceFactory,networkPageSize)
                .setBoundaryCallback(mSubredditBoundaryCallback)
                .build()
        postPageListLiveData.postValue(pageListLiveData)
        refreshStateLiveData.postValue(mSubredditBoundaryCallback.netWorkStatus)
    }
}

