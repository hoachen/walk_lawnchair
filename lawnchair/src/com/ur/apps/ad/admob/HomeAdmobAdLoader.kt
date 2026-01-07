package com.ur.apps.ad.admob

import android.content.Context
import android.view.ViewGroup
import com.ur.apps.utils.URLog

class HomeAdmobAdLoader : AdmobAdLoader() {

    companion object {

        const val TAG = "HomeAdmobAdLoader"

        const val ADMOB_NATIVE_UNIT_ID = "ca-app-pub-2830772598550207/2785508995"

        const val ADMOB_NATIVE_UNIT_ID_TEST = "ca-app-pub-3940256099942544/2247696110"

        const val ADMOB_BANNER_UNIT_ID = "ca-app-pub-2830772598550207/6797083808"

        const val ADMOB_BANNER_UNIT_TEST_ID =  "ca-app-pub-3940256099942544/9214589741"
    }

    override fun enableEventImpressionToTenjin(): Boolean {
        return true
    }

    override fun getBannerAdFormat() : String = "Home_Banner"

    override fun getNativeAdFormat() : String = "Home_Native"

    override fun getNativeUnitId() : String {
        URLog.i(TAG, "getHomeNativeUnitId")
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
        URLog.i(TAG, "load home admob banner ad")
    }

    override fun showBannerAd(context: Context, container: ViewGroup) {
        super.showBannerAd(context, container)
        URLog.i(TAG, "show home admob banner ad")
    }

    override fun loadNativeAd(context: Context) {
        super.loadNativeAd(context)
        URLog.i(TAG, "load home admob native ad")
    }

    override fun showNativeAd(context: Context, adContainer: ViewGroup, adViewWidth: Int) {
        super.showNativeAd(context, adContainer, adViewWidth)
        URLog.i(TAG, "show home admob native ad")
    }

}