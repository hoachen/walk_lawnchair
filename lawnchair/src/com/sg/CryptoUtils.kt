package com.sg

import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import android.util.Base64

object CryptoUtils {
    private const val ALGORITHM = "AES/ECB/PKCS5Padding"
    // 假设密钥为16字节，请根据实际情况配置
    private const val KEY = "8385d19bab7dafef73e8f7cc7340fc67"

    /**
     * 加密后再使用 URL safe base64 编码（将 + 替换为 -，/ 替换为 _，去除尾部的 =）
     */
    fun encrypt(data: String): String {
        try {
            val keySpec = SecretKeySpec(KEY.toByteArray(Charsets.UTF_8), "AES")
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, keySpec)
            val encrypted = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
            
            // 使用Android的Base64工具类，并指定URL_SAFE标志
            var base64 = Base64.encodeToString(encrypted, Base64.NO_WRAP)
            base64 = base64.replace("+", "-").replace("/", "_").replace(Regex("=+$"), "")
            return base64
        } catch (e: Exception) {
            throw RuntimeException("Error during encryption", e)
        }
    }

    /**
     * 将 URL safe 编码转换回标准 base64 后解密
     */
    fun decrypt(encryptedData: String): String {
        try {
            var base64 = encryptedData.replace("-", "+").replace("_", "/")
            val paddingCount = if (base64.length % 4 != 0) 4 - (base64.length % 4) else 0
            repeat(paddingCount) {
                base64 += "="
            }
            val encryptedBytes = Base64.decode(base64, Base64.DEFAULT)
            val keySpec = SecretKeySpec(KEY.toByteArray(Charsets.UTF_8), "AES")
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, keySpec)
            val decrypted = cipher.doFinal(encryptedBytes)
            return String(decrypted, Charsets.UTF_8)
        } catch (e: Exception) {
            throw RuntimeException("Error during decryption", e)
        }
    }
}
