package com.ur.apps.ad

class SAdSDK private constructor()  {
//
//    companion object {
//        const val KEY = "5BMdrQySKe/euQLHaa5dHWN3OWWxncyWyAKZjzsZsHUUL4KTpqD9ZPWDn149iqS8llnLrf8pdlRbfEf25dUOJXDkb8xZ5tCDM5tlPiRhr6Q="
//        // 使用标准的Kotlin懒加载单例模式
//        val instance: SAdSDK by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
//            SAdSDK()
//        }
//    }
//
//    fun onCreate(application : Application) {
////        Log.i("SAdSDK", "call onCreate")
//        URSdk.onApplicationCreate(application) { json ->
//            try {
//                json?.let {
//                    val obj = JSONObject(json)
//                    val event = obj.getString("event")
//                    obj.remove("event")
//                    Log.i("SAdSDK", "reportTrackEvent event=$event,data= $obj")
//                    TDAnalyticsManager.reportTrackEvent(event, obj)
//                }
//            } catch (e: Exception) {
//                e.printStackTrace()
//                Log.i("SAdSDK", "${e.message}")
//            }
//        }
//    }
//
//    fun start(activity: Activity, channel : String) {
//        Log.i("SAdSDK", "call start init $channel")
//        URSdk.start(activity, KEY, channel)
//    }
//
//    fun dispatchTouchEvent(event: MotionEvent) {
//        URSdk.onTouchEvent(event);
//    }
}