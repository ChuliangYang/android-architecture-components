package com.android.example.paging.pagingwithnetwork.reddit.repository.inDb

import androidx.paging.PagingRequestHelper

fun PagingRequestHelper.StatusReport.getErrorMessage():String{
   return PagingRequestHelper.RequestType.values().mapNotNull {
       getErrorFor(it)?.message
   }.first()
}