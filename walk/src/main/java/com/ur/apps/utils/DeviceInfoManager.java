package com.ur.apps.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.webkit.WebSettings;

import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.ur.apps.analysis.utils.DeviceInfoCollector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 设备信息管理类
 * 负责收集和管理设备相关信息，包括设备ID、用户代理、地理位置等
 * 包含完整的国家匹配验证逻辑，防止归因链接欺诈
 */
public class DeviceInfoManager {
    private String guid;
    private String deviceId;
    private String userAgent;
    private String ipAddress;
    private String countryCode;
    private String timezone;
    private String deviceModel;
    private String osVersion;
    private float devicePixelRatio;
    private String buildId;
    private String advertisingId;
    private String currentDate;
    private String currentHour;
    private String packageName;
    private static String TAG = "DeviceInfoManager";
    private static volatile DeviceInfoManager instance;
    private float density;
    private String timeZoneName;

    // 新增字段：运营商和SIM卡信息
    private String carrierCode;
    private String simOperator;
    private String simCountryIso;
    private String networkCountryIso;
    private String mccMnc;
    private boolean hasSimCard;
    private boolean isRoaming;
    private String networkType;
    private int simCount;

    // MCC国家代码映射表
    private static final Map<String, String> MCC_COUNTRY_MAP = new HashMap<>();

    static {
        // 常见MCC代码与国家ISO代码的映射
        MCC_COUNTRY_MAP.put("460", "CN"); // 中国
        MCC_COUNTRY_MAP.put("310", "US"); // 美国
        MCC_COUNTRY_MAP.put("311", "US"); // 美国
        MCC_COUNTRY_MAP.put("234", "GB"); // 英国
        MCC_COUNTRY_MAP.put("440", "JP"); // 日本
        MCC_COUNTRY_MAP.put("450", "KR"); // 韩国
        MCC_COUNTRY_MAP.put("502", "MY"); // 马来西亚
        MCC_COUNTRY_MAP.put("525", "SG"); // 新加坡
        MCC_COUNTRY_MAP.put("228", "CH"); // 瑞士
        MCC_COUNTRY_MAP.put("204", "NL"); // 荷兰
        MCC_COUNTRY_MAP.put("262", "DE"); // 德国
        MCC_COUNTRY_MAP.put("208", "FR"); // 法国
        MCC_COUNTRY_MAP.put("222", "IT"); // 意大利
        MCC_COUNTRY_MAP.put("214", "ES"); // 西班牙
        MCC_COUNTRY_MAP.put("216", "HU"); // 匈牙利
        MCC_COUNTRY_MAP.put("206", "BE"); // 比利时
        MCC_COUNTRY_MAP.put("260", "PL"); // 波兰
        MCC_COUNTRY_MAP.put("232", "AT"); // 奥地利
        MCC_COUNTRY_MAP.put("235", "GB"); // 英国
        MCC_COUNTRY_MAP.put("238", "DK"); // 丹麦
        MCC_COUNTRY_MAP.put("240", "SE"); // 瑞典
        MCC_COUNTRY_MAP.put("242", "NO"); // 挪威
        MCC_COUNTRY_MAP.put("244", "FI"); // 芬兰
        MCC_COUNTRY_MAP.put("246", "LT"); // 立陶宛
        MCC_COUNTRY_MAP.put("247", "LV"); // 拉脱维亚
        MCC_COUNTRY_MAP.put("248", "EE"); // 爱沙尼亚
        MCC_COUNTRY_MAP.put("250", "RU"); // 俄罗斯
        MCC_COUNTRY_MAP.put("255", "UA"); // 乌克兰
        MCC_COUNTRY_MAP.put("257", "BY"); // 白俄罗斯
        MCC_COUNTRY_MAP.put("259", "MD"); // 摩尔多瓦
        MCC_COUNTRY_MAP.put("272", "IE"); // 爱尔兰
        MCC_COUNTRY_MAP.put("274", "IS"); // 冰岛
        MCC_COUNTRY_MAP.put("276", "AL"); // 阿尔巴尼亚
        MCC_COUNTRY_MAP.put("278", "MT"); // 马耳他
        MCC_COUNTRY_MAP.put("280", "CY"); // 塞浦路斯
        MCC_COUNTRY_MAP.put("282", "GE"); // 格鲁吉亚
        MCC_COUNTRY_MAP.put("283", "AM"); // 亚美尼亚
        MCC_COUNTRY_MAP.put("284", "BG"); // 保加利亚
        MCC_COUNTRY_MAP.put("286", "TR"); // 土耳其
        MCC_COUNTRY_MAP.put("288", "FO"); // 法罗群岛
        MCC_COUNTRY_MAP.put("289", "GE"); // 格鲁吉亚
        MCC_COUNTRY_MAP.put("290", "GL"); // 格陵兰
        MCC_COUNTRY_MAP.put("292", "SM"); // 圣马力诺
        MCC_COUNTRY_MAP.put("293", "SI"); // 斯洛文尼亚
        MCC_COUNTRY_MAP.put("294", "MK"); // 北马其顿
        MCC_COUNTRY_MAP.put("295", "LI"); // 列支敦士登
        MCC_COUNTRY_MAP.put("297", "ME"); // 黑山
        MCC_COUNTRY_MAP.put("302", "CA"); // 加拿大
        MCC_COUNTRY_MAP.put("334", "MX"); // 墨西哥
        MCC_COUNTRY_MAP.put("338", "JM"); // 牙买加
        MCC_COUNTRY_MAP.put("340", "GP"); // 瓜德罗普岛
        MCC_COUNTRY_MAP.put("342", "BB"); // 巴巴多斯
        MCC_COUNTRY_MAP.put("344", "AG"); // 安提瓜和巴布达
        MCC_COUNTRY_MAP.put("346", "KY"); // 开曼群岛
        MCC_COUNTRY_MAP.put("348", "VG"); // 英属维尔京群岛
        MCC_COUNTRY_MAP.put("350", "BM"); // 百慕大
        MCC_COUNTRY_MAP.put("352", "GD"); // 格林纳达
        MCC_COUNTRY_MAP.put("354", "MS"); // 蒙特塞拉特
        MCC_COUNTRY_MAP.put("356", "KN"); // 圣基茨和尼维斯
        MCC_COUNTRY_MAP.put("358", "LC"); // 圣卢西亚
        MCC_COUNTRY_MAP.put("360", "VC"); // 圣文森特和格林纳丁斯
        MCC_COUNTRY_MAP.put("362", "AN"); // 荷属安的列斯
        MCC_COUNTRY_MAP.put("363", "AW"); // 阿鲁巴
        MCC_COUNTRY_MAP.put("364", "BS"); // 巴哈马
        MCC_COUNTRY_MAP.put("365", "AI"); // 安圭拉
        MCC_COUNTRY_MAP.put("366", "DM"); // 多米尼克
        MCC_COUNTRY_MAP.put("368", "CU"); // 古巴
        MCC_COUNTRY_MAP.put("370", "DO"); // 多米尼加共和国
        MCC_COUNTRY_MAP.put("372", "HT"); // 海地
        MCC_COUNTRY_MAP.put("374", "TT"); // 特立尼达和多巴哥
        MCC_COUNTRY_MAP.put("376", "TC"); // 特克斯和凯科斯群岛
        MCC_COUNTRY_MAP.put("400", "AZ"); // 阿塞拜疆
        MCC_COUNTRY_MAP.put("401", "KZ"); // 哈萨克斯坦
        MCC_COUNTRY_MAP.put("402", "BT"); // 不丹
        MCC_COUNTRY_MAP.put("404", "IN"); // 印度
        MCC_COUNTRY_MAP.put("405", "IN"); // 印度
        MCC_COUNTRY_MAP.put("410", "PK"); // 巴基斯坦
        MCC_COUNTRY_MAP.put("412", "AF"); // 阿富汗
        MCC_COUNTRY_MAP.put("413", "LK"); // 斯里兰卡
        MCC_COUNTRY_MAP.put("414", "MM"); // 缅甸
        MCC_COUNTRY_MAP.put("415", "LB"); // 黎巴嫩
        MCC_COUNTRY_MAP.put("416", "JO"); // 约旦
        MCC_COUNTRY_MAP.put("417", "SY"); // 叙利亚
        MCC_COUNTRY_MAP.put("418", "IQ"); // 伊拉克
        MCC_COUNTRY_MAP.put("419", "KW"); // 科威特
        MCC_COUNTRY_MAP.put("420", "SA"); // 沙特阿拉伯
        MCC_COUNTRY_MAP.put("421", "YE"); // 也门
        MCC_COUNTRY_MAP.put("422", "OM"); // 阿曼
        MCC_COUNTRY_MAP.put("424", "AE"); // 阿联酋
        MCC_COUNTRY_MAP.put("425", "IL"); // 以色列
        MCC_COUNTRY_MAP.put("426", "BH"); // 巴林
        MCC_COUNTRY_MAP.put("427", "QA"); // 卡塔尔
        MCC_COUNTRY_MAP.put("428", "MN"); // 蒙古
        MCC_COUNTRY_MAP.put("429", "NP"); // 尼泊尔
        MCC_COUNTRY_MAP.put("431", "AE"); // 阿联酋
        MCC_COUNTRY_MAP.put("432", "IR"); // 伊朗
        MCC_COUNTRY_MAP.put("434", "UZ"); // 乌兹别克斯坦
        MCC_COUNTRY_MAP.put("436", "TJ"); // 塔吉克斯坦
        MCC_COUNTRY_MAP.put("437", "KG"); // 吉尔吉斯斯坦
        MCC_COUNTRY_MAP.put("438", "TM"); // 土库曼斯坦
        MCC_COUNTRY_MAP.put("440", "JP"); // 日本
        MCC_COUNTRY_MAP.put("441", "JP"); // 日本
        MCC_COUNTRY_MAP.put("450", "KR"); // 韩国
        MCC_COUNTRY_MAP.put("452", "VN"); // 越南
        MCC_COUNTRY_MAP.put("454", "HK"); // 香港
        MCC_COUNTRY_MAP.put("455", "MO"); // 澳门
        MCC_COUNTRY_MAP.put("456", "KH"); // 柬埔寨
        MCC_COUNTRY_MAP.put("457", "LA"); // 老挝
        MCC_COUNTRY_MAP.put("460", "CN"); // 中国
        MCC_COUNTRY_MAP.put("466", "TW"); // 台湾
        MCC_COUNTRY_MAP.put("467", "KP"); // 朝鲜
        MCC_COUNTRY_MAP.put("470", "BD"); // 孟加拉国
        MCC_COUNTRY_MAP.put("472", "MV"); // 马尔代夫
        MCC_COUNTRY_MAP.put("502", "MY"); // 马来西亚
        MCC_COUNTRY_MAP.put("505", "AU"); // 澳大利亚
        MCC_COUNTRY_MAP.put("510", "ID"); // 印度尼西亚
        MCC_COUNTRY_MAP.put("514", "TL"); // 东帝汶
        MCC_COUNTRY_MAP.put("515", "PH"); // 菲律宾
        MCC_COUNTRY_MAP.put("520", "TH"); // 泰国
        MCC_COUNTRY_MAP.put("525", "SG"); // 新加坡
        MCC_COUNTRY_MAP.put("528", "BN"); // 文莱
        MCC_COUNTRY_MAP.put("530", "NZ"); // 新西兰
        MCC_COUNTRY_MAP.put("536", "NR"); // 瑙鲁
        MCC_COUNTRY_MAP.put("537", "PG"); // 巴布亚新几内亚
        MCC_COUNTRY_MAP.put("539", "TO"); // 汤加
        MCC_COUNTRY_MAP.put("540", "SB"); // 所罗门群岛
        MCC_COUNTRY_MAP.put("541", "VU"); // 瓦努阿图
        MCC_COUNTRY_MAP.put("542", "FJ"); // 斐济
        MCC_COUNTRY_MAP.put("543", "WF"); // 瓦利斯和富图纳
        MCC_COUNTRY_MAP.put("544", "AS"); // 美属萨摩亚
        MCC_COUNTRY_MAP.put("545", "KI"); // 基里巴斯
        MCC_COUNTRY_MAP.put("546", "NC"); // 新喀里多尼亚
        MCC_COUNTRY_MAP.put("547", "PF"); // 法属波利尼西亚
        MCC_COUNTRY_MAP.put("548", "CK"); // 库克群岛
        MCC_COUNTRY_MAP.put("549", "WS"); // 萨摩亚
        MCC_COUNTRY_MAP.put("550", "FM"); // 密克罗尼西亚
        MCC_COUNTRY_MAP.put("552", "PW"); // 帕劳
        MCC_COUNTRY_MAP.put("553", "TV"); // 图瓦卢
        MCC_COUNTRY_MAP.put("555", "NU"); // 纽埃
        MCC_COUNTRY_MAP.put("602", "EG"); // 埃及
        MCC_COUNTRY_MAP.put("603", "DZ"); // 阿尔及利亚
        MCC_COUNTRY_MAP.put("604", "MA"); // 摩洛哥
        MCC_COUNTRY_MAP.put("605", "TN"); // 突尼斯
        MCC_COUNTRY_MAP.put("606", "LY"); // 利比亚
        MCC_COUNTRY_MAP.put("607", "GM"); // 冈比亚
        MCC_COUNTRY_MAP.put("608", "SN"); // 塞内加尔
        MCC_COUNTRY_MAP.put("609", "MR"); // 毛里塔尼亚
        MCC_COUNTRY_MAP.put("610", "ML"); // 马里
        MCC_COUNTRY_MAP.put("611", "GN"); // 几内亚
        MCC_COUNTRY_MAP.put("612", "CI"); // 科特迪瓦
        MCC_COUNTRY_MAP.put("613", "BF"); // 布基纳法索
        MCC_COUNTRY_MAP.put("614", "NE"); // 尼日尔
        MCC_COUNTRY_MAP.put("615", "TG"); // 多哥
        MCC_COUNTRY_MAP.put("616", "BJ"); // 贝宁
        MCC_COUNTRY_MAP.put("617", "MU"); // 毛里求斯
        MCC_COUNTRY_MAP.put("618", "LR"); // 利比里亚
        MCC_COUNTRY_MAP.put("619", "SL"); // 塞拉利昂
        MCC_COUNTRY_MAP.put("620", "GH"); // 加纳
        MCC_COUNTRY_MAP.put("621", "NG"); // 尼日利亚
        MCC_COUNTRY_MAP.put("622", "TD"); // 乍得
        MCC_COUNTRY_MAP.put("623", "CF"); // 中非共和国
        MCC_COUNTRY_MAP.put("624", "CM"); // 喀麦隆
        MCC_COUNTRY_MAP.put("625", "CV"); // 佛得角
        MCC_COUNTRY_MAP.put("626", "ST"); // 圣多美和普林西比
        MCC_COUNTRY_MAP.put("627", "GQ"); // 赤道几内亚
        MCC_COUNTRY_MAP.put("628", "GA"); // 加蓬
        MCC_COUNTRY_MAP.put("629", "CG"); // 刚果共和国
        MCC_COUNTRY_MAP.put("630", "CD"); // 刚果民主共和国
        MCC_COUNTRY_MAP.put("631", "AO"); // 安哥拉
        MCC_COUNTRY_MAP.put("632", "GW"); // 几内亚比绍
        MCC_COUNTRY_MAP.put("633", "SC"); // 塞舌尔
        MCC_COUNTRY_MAP.put("634", "SD"); // 苏丹
        MCC_COUNTRY_MAP.put("635", "RW"); // 卢旺达
        MCC_COUNTRY_MAP.put("636", "ET"); // 埃塞俄比亚
        MCC_COUNTRY_MAP.put("637", "SO"); // 索马里
        MCC_COUNTRY_MAP.put("638", "DJ"); // 吉布提
        MCC_COUNTRY_MAP.put("639", "KE"); // 肯尼亚
        MCC_COUNTRY_MAP.put("640", "TZ"); // 坦桑尼亚
        MCC_COUNTRY_MAP.put("641", "UG"); // 乌干达
        MCC_COUNTRY_MAP.put("642", "BI"); // 布隆迪
        MCC_COUNTRY_MAP.put("643", "MZ"); // 莫桑比克
        MCC_COUNTRY_MAP.put("645", "ZM"); // 赞比亚
        MCC_COUNTRY_MAP.put("646", "MG"); // 马达加斯加
        MCC_COUNTRY_MAP.put("647", "RE"); // 留尼汪
        MCC_COUNTRY_MAP.put("648", "ZW"); // 津巴布韦
        MCC_COUNTRY_MAP.put("649", "NA"); // 纳米比亚
        MCC_COUNTRY_MAP.put("650", "MW"); // 马拉维
        MCC_COUNTRY_MAP.put("651", "LS"); // 莱索托
        MCC_COUNTRY_MAP.put("652", "BW"); // 博茨瓦纳
        MCC_COUNTRY_MAP.put("653", "SZ"); // 斯威士兰
        MCC_COUNTRY_MAP.put("654", "KM"); // 科摩罗
        MCC_COUNTRY_MAP.put("655", "ZA"); // 南非
        MCC_COUNTRY_MAP.put("657", "ER"); // 厄立特里亚
        MCC_COUNTRY_MAP.put("702", "BZ"); // 伯利兹
        MCC_COUNTRY_MAP.put("704", "GT"); // 危地马拉
        MCC_COUNTRY_MAP.put("706", "SV"); // 萨尔瓦多
        MCC_COUNTRY_MAP.put("708", "HN"); // 洪都拉斯
        MCC_COUNTRY_MAP.put("710", "NI"); // 尼加拉瓜
        MCC_COUNTRY_MAP.put("712", "CR"); // 哥斯达黎加
        MCC_COUNTRY_MAP.put("714", "PA"); // 巴拿马
        MCC_COUNTRY_MAP.put("716", "PE"); // 秘鲁
        MCC_COUNTRY_MAP.put("722", "AR"); // 阿根廷
        MCC_COUNTRY_MAP.put("724", "BR"); // 巴西
        MCC_COUNTRY_MAP.put("730", "CL"); // 智利
        MCC_COUNTRY_MAP.put("732", "CO"); // 哥伦比亚
        MCC_COUNTRY_MAP.put("734", "VE"); // 委内瑞拉
        MCC_COUNTRY_MAP.put("736", "BO"); // 玻利维亚
        MCC_COUNTRY_MAP.put("738", "GY"); // 圭亚那
        MCC_COUNTRY_MAP.put("740", "EC"); // 厄瓜多尔
        MCC_COUNTRY_MAP.put("744", "PY"); // 巴拉圭
        MCC_COUNTRY_MAP.put("746", "SR"); // 苏里南
        MCC_COUNTRY_MAP.put("748", "UY"); // 乌拉圭
    }

    // 单例模式
    public static DeviceInfoManager getInstance(Context context) {
        if (instance == null) {
            synchronized (DeviceInfoManager.class) {
                if (instance == null) {
                    instance = new DeviceInfoManager();
                    instance.init(context);
                }
            }
        }
        return instance;
    }

    private void init(Context context) {
        Context appContext = context.getApplicationContext();

        // 初始化基本信息
        this.currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        this.currentHour = new SimpleDateFormat("HH", Locale.getDefault()).format(new Date());
        this.packageName = appContext.getPackageName();

        // 初始化设备信息
        SharedPreferences sp = appContext.getSharedPreferences("LockAdInfo", 0);
        String storedDeviceId = sp.getString("deviceCode", null);
        this.deviceId = storedDeviceId != null ? storedDeviceId : generateAndStoreDeviceId(sp);

        this.deviceModel = Build.MODEL;
        this.osVersion = "Android " + Build.VERSION.RELEASE;
        this.buildId = Build.ID;

        // 初始化UserAgent
        try {
            this.userAgent = WebSettings.getDefaultUserAgent(appContext);
        } catch (Exception e) {
            this.userAgent = "Unknown";
        }

        // 初始化GUID
        String storedGuid = sp.getString("guid", null);
        this.guid = storedGuid != null ? storedGuid : generateAndStoreGuid(sp);

        // 初始化运营商和SIM卡信息
        initCarrierAndSimInfo(appContext);

        // 异步获取Advertising ID
        fetchAdvertisingIdAsync(appContext, sp);
    }

    public final String getPackageName() {
        return this.packageName;
    }

    private void setPackageName(String packageName) {
        this.packageName = packageName;
    }


    public final String getTimeZoneName() {
        return this.timeZoneName;
    }

    public final void setTimeZoneName(String timeZoneName) {
        this.timeZoneName = timeZoneName;
    }

    private String generateAndStoreDeviceId(SharedPreferences sp) {
        String deviceId = UUID.randomUUID().toString();
        sp.edit().putString("deviceCode", deviceId).apply();
        return deviceId;
    }

    private String generateAndStoreGuid(SharedPreferences sp) {
        String guid = UUID.randomUUID().toString();
        sp.edit().putString("guid", guid).apply();
        return guid;
    }

    public String getMccMnc() {
        return mccMnc;
    }

    public String getNetworkType() {
        return networkType;
    }

    public String getNetworkCountryIso() {
        return networkCountryIso;
    }

    public String getSimOperator() {
        return simOperator;
    }

    private void initCarrierAndSimInfo(Context context) {
        try {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (tm == null) {
                return;
            }

            // SIM卡运营商信息
            this.simOperator = tm.getSimOperator();
            this.simCountryIso = tm.getSimCountryIso();
            this.networkCountryIso = tm.getNetworkCountryIso();

            // 解析MCC-MNC
            if (!TextUtils.isEmpty(this.simOperator) && this.simOperator.length() >= 3) {
                String mcc = this.simOperator.substring(0, 3);
                this.carrierCode = mcc;
                this.mccMnc = this.simOperator;

                // 根据MCC推导运营商国家
                if (this.simCountryIso == null || this.simCountryIso.isEmpty()) {
                    String derivedCountry = MCC_COUNTRY_MAP.get(mcc);
                    if (derivedCountry != null) {
                        this.simCountryIso = derivedCountry;
                    }
                }
            }

            // SIM卡状态
            this.hasSimCard = (tm.getSimState() == TelephonyManager.SIM_STATE_READY);
            this.isRoaming = tm.isNetworkRoaming();

            // SIM卡数量（Android 5.1+）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                this.simCount = tm.getPhoneCount();
            } else {
                this.simCount = this.hasSimCard ? 1 : 0;
            }

            URLog.debug(TAG, "Carrier Info: SIM=" + this.simCountryIso +
                    ", Network=" + this.networkCountryIso +
                    ", MCC=" + this.carrierCode +
                    ", Has SIM=" + this.hasSimCard +
                    ", SIM Count=" + this.simCount);

        } catch (SecurityException e) {
            URLog.error(TAG, "No permission to access telephony info");
        } catch (Exception e) {
            URLog.error(TAG, "Failed to init carrier info: " + e.getMessage());
        }
    }

    private void fetchAdvertisingIdAsync(Context context, SharedPreferences sp) {
        AdIdHelper.getAdvertisingId(context, new AdIdHelper.AdIdCallback() {
            @Override
            public void onAdIdRetrieved(String adId) {
                advertisingId = adId;
                sp.edit().putString("gaid", adId).apply();
            }

            @Override
            public void onAdIdError(String error) {
                advertisingId = "";
                sp.edit().putString("gaid", "").apply();
            }
        });
    }

    /**
     * 核心方法：验证国家信息匹配度
     * 综合判断IP国家、SIM卡国家、运营商国家的一致性
     *
     * @param ipCountry 从IP地址解析的国家代码（2位ISO代码）
     * @return 包含验证结果的JSONObject
     */
    public JSONObject validateCountryMatch(String ipCountry) {
        JSONObject result = new JSONObject();

        try {
            // 1. 标准化所有国家代码
            String normalizedIpCountry = normalizeCountryCode(ipCountry);
            String normalizedSimCountry = normalizeCountryCode(this.simCountryIso);
            String normalizedNetworkCountry = normalizeCountryCode(this.networkCountryIso);

            // 2. 从运营商代码推导国家
            String derivedCarrierCountry = getCountryFromMCC(this.carrierCode);

            // 3. 收集所有可用的国家来源
            List<String> availableCountries = new ArrayList<>();
            Map<String, String> countrySources = new HashMap<>();

            if (!TextUtils.isEmpty(normalizedIpCountry)) {
                availableCountries.add(normalizedIpCountry);
                countrySources.put(normalizedIpCountry, "IP");
            }

            if (!TextUtils.isEmpty(normalizedSimCountry)) {
                availableCountries.add(normalizedSimCountry);
                countrySources.put(normalizedSimCountry,
                        countrySources.containsKey(normalizedSimCountry) ?
                                countrySources.get(normalizedSimCountry) + "+SIM" : "SIM");
            }

            if (!TextUtils.isEmpty(normalizedNetworkCountry)) {
                availableCountries.add(normalizedNetworkCountry);
                countrySources.put(normalizedNetworkCountry,
                        countrySources.containsKey(normalizedNetworkCountry) ?
                                countrySources.get(normalizedNetworkCountry) + "+Network" : "Network");
            }

            if (!TextUtils.isEmpty(derivedCarrierCountry)) {
                availableCountries.add(derivedCarrierCountry);
                countrySources.put(derivedCarrierCountry,
                        countrySources.containsKey(derivedCarrierCountry) ?
                                countrySources.get(derivedCarrierCountry) + "+Carrier" : "Carrier");
            }

            // 4. 计算匹配度和可信度
            int totalSources = availableCountries.size();
            Set<String> uniqueCountries = new HashSet<>(availableCountries);

            // 计算匹配分数
            int matchScore = calculateMatchScore(
                    normalizedIpCountry,
                    normalizedSimCountry,
                    normalizedNetworkCountry,
                    derivedCarrierCountry
            );

            // 5. 判断是否匹配
            boolean isMatch = false;
            String determinedCountry = "";
            int confidenceLevel = 0; // 0-100

            if (totalSources == 0) {
                // 无任何国家信息
                determinedCountry = "";
                confidenceLevel = 0;
                isMatch = false;
            } else if (uniqueCountries.size() == 1) {
                // 所有来源国家一致
                determinedCountry = availableCountries.get(0);
                confidenceLevel = calculateConfidenceLevel(matchScore, totalSources, true);
                isMatch = true;
            } else {
                // 国家信息不一致
                determinedCountry = determinePrimaryCountry(
                        normalizedIpCountry,
                        normalizedSimCountry,
                        normalizedNetworkCountry,
                        derivedCarrierCountry,
                        this.hasSimCard,
                        this.isRoaming
                );

                confidenceLevel = calculateConfidenceLevel(matchScore, totalSources, false);
                isMatch = confidenceLevel >= 70; // 置信度70%以上认为匹配
            }

            // 6. 构建结果
            result.put("isMatch", isMatch);
            result.put("confidence", confidenceLevel);
            result.put("determinedCountry", determinedCountry);
            result.put("ipCountry", normalizedIpCountry);
            result.put("simCountry", normalizedSimCountry);
            result.put("networkCountry", normalizedNetworkCountry);
            result.put("carrierCountry", derivedCarrierCountry);
            result.put("carrierCode", this.carrierCode);
            result.put("hasSimCard", this.hasSimCard);
            result.put("isRoaming", this.isRoaming);
            result.put("simCount", this.simCount);
            result.put("uniqueCountryCount", uniqueCountries.size());
            result.put("matchScore", matchScore);

            // 7. 添加警告标志
            result.put("warnings", getWarnings(
                    normalizedIpCountry,
                    normalizedSimCountry,
                    normalizedNetworkCountry,
                    derivedCarrierCountry,
                    this.hasSimCard,
                    this.isRoaming
            ));

            URLog.debug(TAG, "Country Match Result: " + result.toString());

        } catch (JSONException e) {
            URLog.error(TAG, "Failed to build country match result: " + e.getMessage());
        }

        return result;
    }

    /**
     * 标准化国家代码
     */
    private String normalizeCountryCode(String countryCode) {
        if (TextUtils.isEmpty(countryCode)) {
            return "";
        }
        return countryCode.trim().toUpperCase();
    }

    /**
     * 从MCC代码获取国家
     */
    private String getCountryFromMCC(String mcc) {
        if (TextUtils.isEmpty(mcc) || mcc.length() < 3) {
            return "";
        }
        String mccCode = mcc.substring(0, 3);
        return MCC_COUNTRY_MAP.getOrDefault(mccCode, "");
    }

    /**
     * 计算匹配分数
     */
    private int calculateMatchScore(String ipCountry, String simCountry,
                                    String networkCountry, String carrierCountry) {
        int score = 0;

        // IP与SIM卡匹配（权重最高）
        if (!TextUtils.isEmpty(ipCountry) && !TextUtils.isEmpty(simCountry) &&
                ipCountry.equals(simCountry)) {
            score += 30;
        }

        // IP与网络国家匹配
        if (!TextUtils.isEmpty(ipCountry) && !TextUtils.isEmpty(networkCountry) &&
                ipCountry.equals(networkCountry)) {
            score += 20;
        }

        // IP与运营商国家匹配
        if (!TextUtils.isEmpty(ipCountry) && !TextUtils.isEmpty(carrierCountry) &&
                ipCountry.equals(carrierCountry)) {
            score += 25;
        }

        // SIM卡与运营商国家匹配
        if (!TextUtils.isEmpty(simCountry) && !TextUtils.isEmpty(carrierCountry) &&
                simCountry.equals(carrierCountry)) {
            score += 30;
        }

        // SIM卡与网络国家匹配
        if (!TextUtils.isEmpty(simCountry) && !TextUtils.isEmpty(networkCountry) &&
                simCountry.equals(networkCountry)) {
            score += 20;
        }

        return score;
    }

    /**
     * 计算置信度级别
     */
    private int calculateConfidenceLevel(int matchScore, int totalSources, boolean isConsistent) {
        if (totalSources == 0) return 0;

        int baseScore = isConsistent ? 70 : 40;
        int adjustedScore = baseScore + (matchScore / 2);

        // 根据来源数量调整
        if (totalSources >= 3) {
            adjustedScore += 10;
        }

        return Math.min(100, adjustedScore);
    }

    /**
     * 确定主要国家（当信息不一致时）
     */
    private String determinePrimaryCountry(String ipCountry, String simCountry,
                                           String networkCountry, String carrierCountry,
                                           boolean hasSimCard, boolean isRoaming) {
        // 优先级别：SIM卡 > 运营商 > 网络 > IP

        // 1. 如果有SIM卡，优先使用SIM卡国家
        if (hasSimCard && !TextUtils.isEmpty(simCountry)) {
            // 如果是漫游状态，SIM卡国家可能不是当前位置
            if (isRoaming && !TextUtils.isEmpty(networkCountry)) {
                // 漫游时，如果网络国家与IP匹配，则使用网络国家
                if (!TextUtils.isEmpty(ipCountry) && ipCountry.equals(networkCountry)) {
                    return networkCountry;
                }
            }
            return simCountry;
        }

        // 2. 使用运营商推导的国家
        if (!TextUtils.isEmpty(carrierCountry)) {
            return carrierCountry;
        }

        // 3. 使用网络国家
        if (!TextUtils.isEmpty(networkCountry)) {
            return networkCountry;
        }

        // 4. 最后使用IP国家
        return ipCountry != null ? ipCountry : "";
    }

    /**
     * 获取警告信息
     */
    private JSONArray getWarnings(String ipCountry, String simCountry,
                                  String networkCountry, String carrierCountry,
                                  boolean hasSimCard, boolean isRoaming) throws JSONException {
        JSONArray warnings = new JSONArray();

        // 1. 无SIM卡警告
        if (!hasSimCard) {
            warnings.put("NO_SIM_CARD");
        }

        // 2. 漫游警告
        if (isRoaming) {
            warnings.put("ROAMING");
        }

        // 3. 国家不一致警告
        List<String> countries = new ArrayList<>();
        if (!TextUtils.isEmpty(ipCountry)) countries.add(ipCountry);
        if (!TextUtils.isEmpty(simCountry)) countries.add(simCountry);
        if (!TextUtils.isEmpty(networkCountry)) countries.add(networkCountry);
        if (!TextUtils.isEmpty(carrierCountry)) countries.add(carrierCountry);

        Set<String> uniqueCountries = new HashSet<>(countries);
        if (uniqueCountries.size() > 1) {
            warnings.put("COUNTRY_MISMATCH");

            // 具体不匹配类型
            if (!TextUtils.isEmpty(ipCountry) && !TextUtils.isEmpty(simCountry) &&
                    !ipCountry.equals(simCountry)) {
                warnings.put("IP_SIM_MISMATCH");
            }

            if (!TextUtils.isEmpty(ipCountry) && !TextUtils.isEmpty(carrierCountry) &&
                    !ipCountry.equals(carrierCountry)) {
                warnings.put("IP_CARRIER_MISMATCH");
            }
        }

        // 4. VPN/代理嫌疑（IP与所有本地信息都不匹配）
        boolean ipMatchesLocal = false;
        if (!TextUtils.isEmpty(ipCountry)) {
            if (!TextUtils.isEmpty(simCountry) && ipCountry.equals(simCountry)) {
                ipMatchesLocal = true;
            }
            if (!TextUtils.isEmpty(networkCountry) && ipCountry.equals(networkCountry)) {
                ipMatchesLocal = true;
            }
            if (!TextUtils.isEmpty(carrierCountry) && ipCountry.equals(carrierCountry)) {
                ipMatchesLocal = true;
            }

            if (!ipMatchesLocal && hasSimCard) {
                warnings.put("POSSIBLE_VPN");
            }
        }

        return warnings;
    }

    /**
     * 判断指定国家是否开启（核心业务逻辑）
     * @param targetCountry 目标国家代码
     * @param ipCountry 从IP解析的国家代码
     * @return 是否允许开启
     */
    public boolean isCountryEnabled(String targetCountry, String ipCountry) {
        if (TextUtils.isEmpty(targetCountry)) {
            return true; // 无目标国家限制
        }

        // 1. 验证国家匹配
        JSONObject matchResult = validateCountryMatch(ipCountry);

        // 2. 获取最终确定的国家
        String determinedCountry = matchResult.optString("determinedCountry", "");
        int confidence = matchResult.optInt("confidence", 0);
        boolean isMatch = matchResult.optBoolean("isMatch", false);

        // 3. 决策逻辑
        if (TextUtils.isEmpty(determinedCountry)) {
            // 无法确定国家
            URLog.warning(TAG, "Cannot determine country, using IP as fallback");
            return targetCountry.equalsIgnoreCase(normalizeCountryCode(ipCountry));
        }

        // 4. 基于置信度的决策
        if (confidence >= 80) {
            // 高置信度：直接使用确定的国家
            return targetCountry.equalsIgnoreCase(determinedCountry);
        } else if (confidence >= 60) {
            // 中等置信度：需要额外检查
            boolean matchesTarget = targetCountry.equalsIgnoreCase(determinedCountry);

            // 检查是否有VPN警告
            JSONArray warnings = matchResult.optJSONArray("warnings");
            boolean hasVPNWarning = false;
            if (warnings != null) {
                for (int i = 0; i < warnings.length(); i++) {
                    if ("POSSIBLE_VPN".equals(warnings.optString(i))) {
                        hasVPNWarning = true;
                        break;
                    }
                }
            }

            // 如果有VPN嫌疑，更严格检查
            if (hasVPNWarning && !matchesTarget) {
                return false;
            }

            return matchesTarget;
        } else {
            // 低置信度：保守策略
            URLog.warning(TAG, "Low confidence in country determination: " + confidence);

            // 检查所有可能的国家
            String ipCountryNorm = normalizeCountryCode(ipCountry);
            String simCountryNorm = normalizeCountryCode(this.simCountryIso);
            String carrierCountryNorm = getCountryFromMCC(this.carrierCode);

            // 只要有一个匹配就允许（保守策略）
            boolean ipMatches = targetCountry.equalsIgnoreCase(ipCountryNorm);
            boolean simMatches = targetCountry.equalsIgnoreCase(simCountryNorm);
            boolean carrierMatches = targetCountry.equalsIgnoreCase(carrierCountryNorm);

            return ipMatches || simMatches || carrierMatches;
        }
    }

    public final Map<String, String> buildRegisterRequestBodyMap() {
        Map<String, String> map = new HashMap<>();
        try {
            map.put("guid", this.guid);
            map.put("deviceId", this.deviceId);
            map.put("ua", this.userAgent);
            map.put("ip", this.ipAddress);
            map.put("country", this.countryCode);
            map.put("timezone", this.timezone);
            map.put("deviceModel", this.deviceModel);
            map.put("osVersion", this.osVersion);
            map.put("dpr", String.valueOf(this.devicePixelRatio));
            map.put("buildId", this.buildId);
            map.put("gaid", this.advertisingId != null ? this.advertisingId : "");
            // 运营商和SIM卡信息
            map.put("carrierCode", this.carrierCode != null ? this.carrierCode : "");
            map.put("simOperator", this.simOperator != null ? this.simOperator : "");
            map.put("simCountryIso", this.simCountryIso != null ? this.simCountryIso : "");
            map.put("networkCountryIso", this.networkCountryIso != null ? this.networkCountryIso : "");
            map.put("mccMnc", this.mccMnc != null ? this.mccMnc : "");
            map.put("hasSimCard", String.valueOf(this.hasSimCard));
            map.put("isRoaming", String.valueOf(this.isRoaming));
            map.put("simCount", String.valueOf(this.simCount));
            map.put("networkType", this.networkType != null ? this.networkType : "");

            URLog.debug(TAG, "Register Request Body: " + map);
            return map;

        } catch (Exception e) {
            URLog.error(TAG, "Build Request Body failed: " + e.getMessage());
        }
        return map;
    }

    /**
     * 构建设备注册请求的JSON数据（包含完整验证信息）
     */
    public final String buildRegisterRequestBody() {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("guid", this.guid);
            jsonObject.put("deviceId", this.deviceId);
            jsonObject.put("ua", this.userAgent);
            jsonObject.put("ip", this.ipAddress);
            jsonObject.put("country", this.countryCode);
            jsonObject.put("timezone", this.timezone);
            jsonObject.put("deviceModel", this.deviceModel);
            jsonObject.put("osVersion", this.osVersion);
            jsonObject.put("dpr", this.devicePixelRatio);
            jsonObject.put("buildId", this.buildId);
            jsonObject.put("gaid", this.advertisingId != null ? this.advertisingId : "");
            // 运营商和SIM卡信息
            jsonObject.put("carrierCode", this.carrierCode != null ? this.carrierCode : "");
            jsonObject.put("simOperator", this.simOperator != null ? this.simOperator : "");
            jsonObject.put("simCountryIso", this.simCountryIso != null ? this.simCountryIso : "");
            jsonObject.put("networkCountryIso", this.networkCountryIso != null ? this.networkCountryIso : "");
            jsonObject.put("mccMnc", this.mccMnc != null ? this.mccMnc : "");
            jsonObject.put("hasSimCard", this.hasSimCard);
            jsonObject.put("isRoaming", this.isRoaming);
            jsonObject.put("simCount", this.simCount);

//            // 国家验证结果
//            if (!TextUtils.isEmpty(ipCountry)) {
//                JSONObject countryValidation = validateCountryMatch(ipCountry);
//                jsonObject.put("countryValidation", countryValidation);
//            }

            String requestBody = jsonObject.toString();
            URLog.debug(TAG, "Register Request Body: " + requestBody);
            return requestBody;

        } catch (JSONException e) {
            URLog.error(TAG, "Build Request Body failed: " + e.getMessage());
            return "{}";
        }
    }

    // Getter 方法
    public String getDeviceId() { return deviceId; }
    public String getUserAgent() { return userAgent; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getCountryCode() { return countryCode; }

    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
    public String getAdvertisingId() { return advertisingId; }
    public String getCarrierCode() { return carrierCode; }
    public String getSimCountryIso() { return simCountryIso; }
    public boolean getHasSimCard() { return hasSimCard; }
    public boolean getIsRoaming() { return isRoaming; }
    public int getSimCount() { return simCount; }

    public static class AdIdHelper {
        private static final String TAG = "AdIdHelper";

        /**
         * 广告ID回调接口
         */
        public interface AdIdCallback {
            void onAdIdRetrieved(String adId);
            void onAdIdError(String error);
        }

        private AdIdHelper() {
        }

        /**
         * 异步获取广告ID
         * @param context 上下文对象
         * @param callback 回调接口
         */
        public static void getAdvertisingId(Context context, AdIdCallback callback) {
            new Thread(() -> {
                try {
                    String cacheAdvertisingId = DeviceInfoCollector.Companion.getInstance().getAdvertisingId();
                    if (!TextUtils.isEmpty(cacheAdvertisingId) && callback != null) {
                        callback.onAdIdRetrieved(cacheAdvertisingId);
                        return;
                    }
                    AdvertisingIdClient.Info advertisingIdInfo = AdvertisingIdClient.getAdvertisingIdInfo(context.getApplicationContext());
                    if (advertisingIdInfo == null) {
                        callback.onAdIdError("Failed to retrieve GAID");
                        return;
                    }
                    String adId = advertisingIdInfo.getId();
                    if (!advertisingIdInfo.isLimitAdTrackingEnabled()) {
                        callback.onAdIdRetrieved(adId);
                    } else {
                        URLog.warning(TAG, "Ad tracking is limited by user");
                        callback.onAdIdError("Ad tracking is limited by user");
                    }
                }  catch (GooglePlayServicesRepairableException e) {
                    callback.onAdIdError("Google Play Services repairable error: " + e.getMessage());
                } catch (IOException e) {
                    callback.onAdIdError("IOException while fetching GAID: " + e.getMessage());
                } catch (GooglePlayServicesNotAvailableException e) {
                    callback.onAdIdError("Google Play Services not available: " + e.getMessage());
                } catch (Exception e) {
                    callback.onAdIdError("Unexpected error while fetching GAID: " + e.getMessage());
                }
            }).start();
        }
    }
}

