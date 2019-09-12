package com.android.example.paging.pagingwithnetwork.reddit.vo

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "Reddit_Post", indices = arrayOf(Index(value = ["subreddit"])))
data class mRedditPost(
        @PrimaryKey
        val name:String,
        val title:String,
        val scroe:Int,
        val author:String,
        val subreddit:String,
        val num_comments:Int,
        @ColumnInfo(name="test", index = true)
        val createTme:Long
        )

