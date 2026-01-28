/*
 * Copyright 2021, Lawnchair
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package app.lawnchair

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import app.lawnchair.backup.LawnchairBackup
import app.lawnchair.flowerpot.Flowerpot
import app.lawnchair.preferences.PreferenceManager
import app.lawnchair.ui.ModalBottomSheetContent
import app.lawnchair.ui.preferences.destinations.openAppInfo
import app.lawnchair.util.restartLauncher
import app.lawnchair.util.unsafeLazy
import app.lawnchair.views.ComposeBottomSheet
import com.android.launcher3.BuildConfig
import com.android.launcher3.InvariantDeviceProfile
import com.android.launcher3.Launcher
import com.android.launcher3.R
import com.android.launcher3.Utilities
import com.android.quickstep.RecentsActivity
import com.android.systemui.shared.system.QuickStepContract
import java.io.File

class LawnchairApp : Application() {

    override fun onCreate() {
        super.onCreate()
        LauncherSDK.init(this)
        LauncherSDK.onLauncherAppStateCreated(this)
    }

    companion object {
        @JvmStatic
        val instance: LauncherSDK get() = LauncherSDK
        @JvmStatic
        val isRecentsEnabled: Boolean get() = LauncherSDK.recentsEnabled
        @JvmStatic
        val isAtleastT: Boolean get() = LauncherSDK.isAtleastT

        @JvmStatic
        fun getUriForFile(context: Context, file: File): Uri {
            return LauncherSDK.getUriForFile(context, file)
        }
    }
}

val Context.lawnchairApp get() = LauncherSDK
