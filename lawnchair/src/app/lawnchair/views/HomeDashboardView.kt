package app.lawnchair.views

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Process
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextClock

/** Launcher-owned home header; unlike Smartspace it is available even without Google services. */
class HomeDashboardView(context: Context) : FrameLayout(context) {
    init {
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(20), dp(12), dp(20), dp(12))
            background = GradientDrawable().apply {
                // Opaque enough to keep the user workspace icons from visually bleeding through
                // the Launcher-owned header.
                setColor(Color.argb(232, 20, 28, 38))
                cornerRadius = dp(28).toFloat()
            }
        }
        addView(card, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        card.addView(TextClock(context).apply {
            format12Hour = "hh:mm"
            format24Hour = "HH:mm"
            setTextColor(Color.WHITE)
            textSize = 52f
            gravity = Gravity.CENTER
            includeFontPadding = false
        })
        card.addView(TextClock(context).apply {
            format12Hour = "M-d EEEE"
            format24Hour = "M-d EEEE"
            setTextColor(Color.WHITE)
            textSize = 20f
            gravity = Gravity.CENTER
            includeFontPadding = false
        })
        val apps = LinearLayout(context).apply {
            gravity = Gravity.CENTER
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(10), 0, 0)
        }
        card.addView(apps, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(54)))
        addFrequentSystemApps(apps)
    }

    private fun addFrequentSystemApps(row: LinearLayout) {
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val user = Process.myUserHandle()
        val intents = listOf(
            Intent(Intent.ACTION_DIAL),
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MESSAGING),
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CONTACTS),
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_BROWSER),
            Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA),
        )
        val seen = hashSetOf<String>()
        intents.forEach { intent ->
            val resolvedComponent = intent.resolveActivity(context.packageManager) ?: return@forEach
            if (!seen.add(resolvedComponent.packageName)) return@forEach
            val activity = launcherApps.getActivityList(resolvedComponent.packageName, user)
                .firstOrNull { it.componentName == resolvedComponent } ?: return@forEach
            row.addView(ImageView(context).apply {
                setImageDrawable(activity.getBadgedIcon(0))
                contentDescription = activity.label
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                setOnClickListener {
                    context.startActivity(Intent(Intent.ACTION_MAIN).apply {
                        setComponent(activity.componentName)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                }
            }, LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
        }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
