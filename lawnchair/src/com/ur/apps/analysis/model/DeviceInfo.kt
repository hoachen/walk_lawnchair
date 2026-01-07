package com.ur.apps.analysis.model

data class DeviceInfo(
    val isOpenDebug: Boolean = false,
    val isRunEmu: Boolean = false,
    val emuCheckReason: String? = null,
    val isRunVirtual: Boolean = false,
    val isVpnOpen: Boolean = false,
    val isRooted: Boolean = false
)
