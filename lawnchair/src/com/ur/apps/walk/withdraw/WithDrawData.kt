package com.ur.apps.walk.withdraw

import com.sg.response.FormField

class WithDrawData {
    fun isInvalid(): Boolean {
        return paymentId == -1L || paymentProviderItemId == -1L
    }

    var paymentId: Long = -1L
    var paymentProviderItemId: Long = -1L

    var formFields: List<FormField> = listOf()

    var udCoin: Long = 0L
    var udAmount: Double = 0.0
}