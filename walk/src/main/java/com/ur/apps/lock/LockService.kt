package com.ur.apps.lock

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface LockService {

    @GET("/")
    fun getConfig(@Query("pkg") pkg: String,
                  @Query("v") version: String): Call<LockAdConfig>
}