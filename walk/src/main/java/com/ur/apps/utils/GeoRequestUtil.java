package com.ur.apps.utils;

import com.ur.apps.analysis.td.TDAnalyticsManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GeoRequestUtil {

    private static String TAG = "GeoRequestUtil";

    private static String EVENT_GEO_REQUEST = "GeoRequest";

    private static final String[] GEO_API_URLS = {
            "https://whoami04.royadata.io/ipgeo",
            "https://whoami05.royadata.io/ipgeo",
            "https://whoami06.royadata.io/ipgeo"
    };

    private void reportRequestEvent(boolean isSuccuess,
                                    String url,
                                    String error, long costTime) {
        JSONObject object = new JSONObject();
        try {
            object.put("is_success", isSuccuess);
            object.put("error", error);
            object.put("costTime", costTime);
            TDAnalyticsManager.INSTANCE.reportTrackEvent(EVENT_GEO_REQUEST,object);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public void fetchGeoInformation(DeviceInfoManager deviceInfoManager, final Runnable completionCallback) {
        fetchGeoInformation(new ArrayList<>(Arrays.asList(GEO_API_URLS)),
                deviceInfoManager, 0, completionCallback);
    }

    public void fetchGeoInformation(List<String> apiUrls,
                                    DeviceInfoManager deviceInfoManager,
                                    int currentIndex,
                                    final Runnable completionCallback) {
        if (currentIndex >= apiUrls.size()) {
            URLog.error(TAG, "All geo info URL requests failed");
            return;
        }
        long startTime = System.currentTimeMillis();
        String currentUrl = apiUrls.get(currentIndex);
        URLog.debug(TAG, String.format("Try request %s (%d/%d)", currentUrl, currentIndex + 1, apiUrls.size()));

        try {
            final List<String> urlList = apiUrls;
            final int nextIndex = currentIndex;

            HttpClient.makeGetRequest(currentUrl, new HttpClient.ResponseCallback() {
                @Override
                public void onSuccess(String response) {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        int code = jsonResponse.getInt("code");
                        if (code != 0) {
                            String errorMsg = jsonResponse.getString("msg");
                            URLog.error(TAG, "Geo request failed: code=" + code + ", msg=" + errorMsg);
                            fetchGeoInformation(urlList, deviceInfoManager, nextIndex + 1, completionCallback);
                            reportRequestEvent(false, currentUrl,
                                    errorMsg,System.currentTimeMillis() - startTime);
                            return;
                        }
                        reportRequestEvent(true, currentUrl, "",System.currentTimeMillis() - startTime);
                        JSONObject data = jsonResponse.getJSONObject("data");
                        deviceInfoManager.setIpAddress(data.optString("ip"));
                        deviceInfoManager.setCountryCode(data.optString("country_code"));
                        deviceInfoManager.setTimeZoneName(data.optString("zone_name"));

                        URLog.debug(TAG, "Geo info fetched: country_code=" + deviceInfoManager.getCountryCode() +
                                ", ip=" + deviceInfoManager.getIpAddress() +
                                "timeZone:" + deviceInfoManager.getTimeZoneName());

                        if (completionCallback != null) {
                            completionCallback.run();
                        }
                    } catch (JSONException e) {
                        URLog.error(TAG, "Parse JSON failed: " + e.getMessage());
                        reportRequestEvent(false, currentUrl, e.getMessage(), System.currentTimeMillis() - startTime);
                        fetchGeoInformation(urlList, deviceInfoManager, nextIndex + 1, completionCallback);
                    }
                }

                @Override
                public void onError(String e) {
                    URLog.error(TAG, "Parse JSON failed: " + e);
                    reportRequestEvent(false, currentUrl, e, System.currentTimeMillis() - startTime);
                    fetchGeoInformation(urlList, deviceInfoManager, nextIndex + 1, completionCallback);
                }
            });
        } catch (Exception e) {
            URLog.error(TAG,  "Request failed: " + e.getMessage());
            reportRequestEvent(false, currentUrl, e.getMessage(), System.currentTimeMillis() - startTime);
            fetchGeoInformation(apiUrls, deviceInfoManager, currentIndex + 1, completionCallback);
        }
    }
}
