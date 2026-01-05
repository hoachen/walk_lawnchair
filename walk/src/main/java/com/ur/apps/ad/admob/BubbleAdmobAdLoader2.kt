package com.ur.apps.ad.admob

import android.content.Context
import android.view.ViewGroup
import com.ur.apps.utils.URLog

class BubbleAdmobAdLoader2 : AdmobAdLoader() {

    companion object {

        const val TAG = "BubbleAdmobAdLoader2"
        const val ADMOB_NATIVE_UNIT_ID = "ca-app-pub-2830772598550207/2662397670"
        const val ADMOB_NATIVE_UNIT_ID_TEST = "ca-app-pub-3940256099942544/2247696110"
        const val ADMOB_BANNER_UNIT_ID = "ca-app-pub-2830772598550207/5337303104"
        const val ADMOB_BANNER_UNIT_TEST_ID =  "ca-app-pub-3940256099942544/9214589741"

    }


    override fun getBannerAdFormat() : String = "Out_Banner"

    override fun getNativeAdFormat() : String = "Out_Native"

    override fun enableEventImpressionToTenjin() = false

    override fun loggerTag(): String {
        return TAG
    }


    override fun getNativeUnitId() : String {
        URLog.i(TAG, "getBoobleNativeUnitId")
        if (useDebugAdmobId) {
            URLog.i(TAG, "user Debug Admob native unit id $ADMOB_NATIVE_UNIT_ID_TEST")
            return ADMOB_NATIVE_UNIT_ID_TEST
        }
        return ADMOB_NATIVE_UNIT_ID
    }

    override fun getBannerUnitId() : String {
        URLog.i(TAG, "getHomeBannerUnitId")
        if (useDebugAdmobId) {
            URLog.i(TAG, "user Debug Admob banner unit id $ADMOB_BANNER_UNIT_TEST_ID")
            return ADMOB_BANNER_UNIT_TEST_ID
        }
        return ADMOB_BANNER_UNIT_ID
    }


    override fun loadBannerAd(context: Context, container: ViewGroup?) {
        super.loadBannerAd(context, container)
        URLog.i(TAG, "load bubble admob banner ad")
    }

    override fun showBannerAd(context: Context, container: ViewGroup) {
        super.showBannerAd(context, container)
        URLog.i(TAG, "show bubble admob banner ad")
    }

    override fun loadNativeAd(context: Context) {
        super.loadNativeAd(context)
        URLog.i(TAG, "load bubble admob native ad")
    }

    override fun showNativeAd(context: Context, adContainer: ViewGroup, adViewWidth: Int) {
        super.showNativeAd(context, adContainer, adViewWidth)
        URLog.i(TAG, "show bubble admob native ad")
    }

}