package com.sg

data class BaseResponse<T>(
    val code: String,
    val msg: String,
    val uid: String,
    val st: Long,
    val data: T?
)
