package com.sg

class ApiException(val code: String, message: String) : Exception(message)
