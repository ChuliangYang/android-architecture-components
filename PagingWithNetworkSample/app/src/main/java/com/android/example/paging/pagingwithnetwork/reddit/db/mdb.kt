package com.android.example.paging.pagingwithnetwork.reddit.db

import android.content.Context
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Room
import androidx.room.RoomDatabase
import com.android.example.paging.pagingwithnetwork.reddit.vo.mRedditPost

@Database(entities = arrayOf(mRedditPost::class),version = 1)
abstract class PostDb:RoomDatabase(){

    abstract fun getRedditDao():RedditPostDao

    companion object{

        @Volatile var INSTANCE:PostDb?=null

        fun getInstance(context:Context):PostDb{
            if(INSTANCE==null){
                synchronized(this){
                    if(INSTANCE==null){
                        INSTANCE= Room.databaseBuilder(context,PostDb::class.java,"test").build()
                    }
                }
            }
        }
    }
}