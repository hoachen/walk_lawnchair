package com.ur.apps.ad.admob

import android.app.Activity
import com.chartboost.sdk.Chartboost
import com.chartboost.sdk.privacy.model.GDPR
import com.google.ads.mediation.inmobi.InMobiConsent
import com.inmobi.sdk.InMobiSdk
import com.unity3d.ads.metadata.MetaData
import com.vungle.ads.VunglePrivacySettings
import org.json.JSONException
import org.json.JSONObject

object AdmobMediationAdapter {


    public fun init(context: Activity) {
        // [START set_gdpr_meta_data]
        val gdprMetaData = MetaData(context)
        gdprMetaData["gdpr.consent"] = true
        gdprMetaData.commit()
        // [END set_gdpr_meta_data]
        VunglePrivacySettings.setCCPAStatus(true)

        val dataUseConsent = GDPR(GDPR.GDPR_CONSENT.NON_BEHAVIORAL)
        Chartboost.addDataUseConsent(context, dataUseConsent)
//        InneractiveAdManager.setUSPrivacyString(US_PRIVACY_STRING)

//        PangleMediationAdapter.setPAConsent(PAGConstant.PAGPAConsentType.PAG_PA_CONSENT_TYPE_CONSENT)

        val consentObject = JSONObject()
        try {
            consentObject.put(InMobiSdk.IM_GDPR_CONSENT_AVAILABLE, true)
            consentObject.put("gdpr", "1")
        } catch (exception: JSONException) {
        }
        InMobiConsent.updateGDPRConsent(consentObject)
    }

    public fun setCcpaMetaData(context: Activity) {
        // [START set_ccpa_meta_data]
        val ccpaMetaData = MetaData(context)
        ccpaMetaData["privacy.consent"] = true
        ccpaMetaData.commit()
        // [END set_ccpa_meta_data]
    }
}