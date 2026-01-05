package com.ur.apps.utils;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

/**
 * HTTP客户端工具类
 * 负责处理HTTP请求，支持GET/POST方法、重试机制和GZIP压缩
 */
public final class HttpClient {
    private static final int MAX_RETRY_COUNT = 3;
    private static final long RETRY_DELAY_MS = 30000;
    private static final String TAG = "HttpClient";
    private static final int CONNECT_TIMEOUT_MS = (int) TimeUnit.SECONDS.toMillis(5);
    private static final int READ_TIMEOUT_MS = (int) TimeUnit.SECONDS.toMillis(5);

    /**
     * HTTP响应回调接口
     */
    public interface ResponseCallback {
        void onSuccess(String response);
        void onError(String error);
    }

    /**
     * 执行GET请求
     * @param url 请求URL
     * @param callback 响应回调
     */
    public static void makeGetRequest(String url, ResponseCallback callback) {
        executeRequest("GET", url, null, null, callback, "getGeoInfo");
    }

    /**
     * 执行带参数的GET请求
     * @param url 请求URL
     * @param headers 请求头
     * @param position 位置参数
     * @param country 国家参数
     * @param testPackage 测试包名
     * @param callback 响应回调
     */
    public static void makeGetRequest(String url, Map<String, String> headers, String position, 
                                     String country, String testPackage, ResponseCallback callback) {
        Uri.Builder uriBuilder = Uri.parse(url).buildUpon();
        URLog.debug(TAG, "Request URL: pos=" + position);
        
        if (!TextUtils.isEmpty(position)) {
            uriBuilder.appendQueryParameter("position", position);
        }
        if (!TextUtils.isEmpty(country)) {
            uriBuilder.appendQueryParameter("country", country);
        }
        if (!TextUtils.isEmpty(testPackage)) {
            uriBuilder.appendQueryParameter("testPkg", testPackage);
        }
        
        String finalUrl = uriBuilder.build().toString();
        URLog.debug(TAG, "Build GET request URL: " + finalUrl);
        executeRequest("GET", finalUrl, headers, null, callback, "getAsync");
    }

    /**
     * 执行带测试包名的GET请求
     * @param url 请求URL
     * @param headers 请求头
     * @param callback 响应回调
     * @param testPackage 测试包名
     */
    private static void makeGetRequestWithTestPackage(String url, Map<String, String> headers, 
                                                     ResponseCallback callback, String testPackage) {
        try {
            Uri.Builder uriBuilder = Uri.parse(url).buildUpon();
            if (!TextUtils.isEmpty(testPackage)) {
                uriBuilder.appendQueryParameter("testPkg", testPackage);
            }
            String finalUrl = uriBuilder.build().toString();
            URLog.debug(TAG, "getAProcess request URL: " + finalUrl);
            executeRequest("GET", finalUrl, headers, null, callback, "getAProcess");
        } catch (Throwable th) {
            URLog.error(TAG, "getAProcess build URL failed: " + th.getMessage());
            if (callback != null) {
                callback.onError("Invalid url: " + th.getMessage());
            }
        }
    }

    /**
     * 执行POST请求
     * @param url 请求URL
     * @param headers 请求头
     * @param requestBody 请求体
     * @param callback 响应回调
     */
    public static void makePostRequest(String url, Map<String, String> headers, 
                                      String requestBody, ResponseCallback callback) {
        executeRequest("POST", url, headers, requestBody, callback, "postAsync");
    }

    /**
     * 执行HTTP请求
     * @param method 请求方法（GET/POST等）
     * @param url 请求URL
     * @param headers 请求头
     * @param requestBody 请求体
     * @param callback 响应回调
     * @param requestType 请求类型标识
     */
    private static void executeRequest(String method, String url, Map<String, String> headers,
                                     String requestBody, ResponseCallback callback, String requestType) {
        if (callback == null) {
            URLog.error(TAG, "Callback is null at start of request: " + url);
            return;
        }
        
        new Thread(() -> {
            int retryCount = 0;
            while (retryCount < MAX_RETRY_COUNT) {
                retryCount++;
                HttpURLConnection connection = null;
                
                try {
                    URLog.debug(TAG, String.format("Execute request: %s (try %d/%d) %s",
                        url, retryCount, MAX_RETRY_COUNT, requestType));
                    
                    connection = (HttpURLConnection) new URL(url).openConnection();
                    setupConnection(connection, method, headers);
                    
                    // 处理请求体（POST/PUT/PATCH方法）
                    if (isRequestBodyMethod(method) && requestBody != null) {
                        sendRequestBody(connection, requestBody);
                    }
                    
                    int responseCode = connection.getResponseCode();
                    String responseBody = readResponseBody(connection, responseCode);

                    URLog.debug(TAG, "Response: " + responseBody);
                    
                    if (responseCode >= 200 && responseCode < 300) {
                        // 请求成功
                        handleSuccessResponse(responseBody, callback);
                        if (connection != null) {
                            connection.disconnect();
                        }
                        return;
                    } else {
                        // HTTP错误
                        String errorMessage = "HTTP Error: " + responseCode + " body=" + responseBody;
                        URLog.error(TAG, errorMessage);
                        
                        if (retryCount == MAX_RETRY_COUNT) {
                            handleErrorResponse(errorMessage, callback);
                            if (connection != null) {
                                connection.disconnect();
                            }
                            return;
                        }
                    }
                    
                } catch (Exception e) {
                    URLog.error(TAG, String.format("Request failed: %s (try %d/%d) %s",
                        e.getMessage(), retryCount, MAX_RETRY_COUNT, requestType));
                    
                    if (retryCount == MAX_RETRY_COUNT) {
                        handleErrorResponse(e.getMessage(), callback);
                        if (connection != null) {
                            connection.disconnect();
                        }
                        return;
                    }
                    
                    // 重试前等待
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException e2) {
                        URLog.error(TAG, "Retry wait interrupted: " + e2.getMessage());
                        Thread.currentThread().interrupt();
                        if (connection != null) {
                            connection.disconnect();
                        }
                        return;
                    }
                } finally {
                    if (connection != null) {
                        try {
                            connection.disconnect();
                        } catch (Exception e) {
                            // 忽略断开连接时的异常
                        }
                    }
                }
            }
        }).start();
    }

    /**
     * 设置HTTP连接参数
     * @param connection HTTP连接
     * @param method 请求方法
     * @param headers 请求头
     */
    private static void setupConnection(HttpURLConnection connection, String method, 
                                       Map<String, String> headers) throws Exception {
        connection.setRequestMethod(method);
        connection.setInstanceFollowRedirects(true);
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setUseCaches(false);
        
        // 设置默认请求头
        connection.setRequestProperty("Accept", "application/json, text/plain, */*");
        connection.setRequestProperty("Accept-Encoding", "gzip");
        
        // 设置自定义请求头
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                connection.setRequestProperty(entry.getKey(), entry.getValue());
            }
        }
        
        // 对于需要请求体的方法，设置输出流
        if (isRequestBodyMethod(method)) {
            connection.setDoOutput(true);
            if (connection.getRequestProperty("Content-Type") == null) {
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            }
        }
    }

    /**
     * 检查是否为需要请求体的方法
     * @param method HTTP方法
     * @return 是否需要请求体
     */
    private static boolean isRequestBodyMethod(String method) {
        return "POST".equalsIgnoreCase(method) || 
               "PUT".equalsIgnoreCase(method) || 
               "PATCH".equalsIgnoreCase(method);
    }

    /**
     * 发送请求体
     * @param connection HTTP连接
     * @param requestBody 请求体内容
     */
    private static void sendRequestBody(HttpURLConnection connection, String requestBody) throws Exception {
        OutputStream outputStream = connection.getOutputStream();
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));
            try {
                writer.write(requestBody);
                writer.flush();
            } finally {
                writer.close();
            }
        } finally {
            outputStream.close();
        }
    }

    /**
     * 读取响应体
     * @param connection HTTP连接
     * @param responseCode 响应码
     * @return 响应体内容
     */
    private static String readResponseBody(HttpURLConnection connection, int responseCode) throws Exception {
        InputStream inputStream = (responseCode >= 200 && responseCode < 300) ? 
                                 connection.getInputStream() : connection.getErrorStream();
        
        if (inputStream == null) {
            return "";
        }
        
        // 处理GZIP压缩
        String contentEncoding = connection.getHeaderField("Content-Encoding");
        if (contentEncoding != null && contentEncoding.toLowerCase().contains("gzip")) {
            inputStream = new GZIPInputStream(inputStream);
        }
        
        return readInputStreamToString(inputStream);
    }

    /**
     * 从输入流读取字符串
     * @param inputStream 输入流
     * @return 字符串内容
     */
    private static String readInputStreamToString(InputStream inputStream) {
        if (inputStream == null) {
            return "";
        }
        
        StringBuilder response = new StringBuilder();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            return response.toString();
        } catch (Exception e) {
            URLog.error(TAG, "Read response stream failed: " + e.getMessage());
            return "";
        } finally {
            try {
                inputStream.close();
            } catch (Exception e) {
                // 忽略关闭流时的异常
            }
        }
    }

    /**
     * 处理成功响应
     * @param response 响应内容
     * @param callback 回调接口
     */
    private static void handleSuccessResponse(String response, ResponseCallback callback) {

        new Handler(Looper.getMainLooper()).post(() -> {
            if (callback != null) {
                callback.onSuccess(response);
            }
        });
    }

    /**
     * 处理错误响应
     * @param error 错误信息
     * @param callback 回调接口
     */
    private static void handleErrorResponse(String error, ResponseCallback callback) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (callback != null) {
                callback.onError(error);
            }
        });
    }
}
