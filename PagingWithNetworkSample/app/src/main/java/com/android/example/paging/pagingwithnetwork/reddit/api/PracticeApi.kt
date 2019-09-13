package com.android.example.paging.pagingwithnetwork.reddit.api

import com.android.example.paging.pagingwithnetwork.reddit.vo.RedditPost
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.*


//base url http
interface PracticeApi {
    @GET("/api/News/{id}/{type}")
    fun getNews(
            @Path("id") id:Int,
            @Path("type") type:String,
            @Query("key") value:String
    ):Call<RedditPost>

    @Multipart
    @POST("/command/123")
    fun post(
            @Field("key") value:String,
            @Part("123") part:RedditPost,
            @Part file:MultipartBody.Part
    )
}