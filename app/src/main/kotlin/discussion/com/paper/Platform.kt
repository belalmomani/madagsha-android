package discussion.com.paper

import android.app.Activity
import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.startapp.sdk.adsbase.StartAppAd
import com.startapp.sdk.adsbase.StartAppSDK
import discussion.com.paper.core.AdBackend
import discussion.com.paper.core.LocalStore
import discussion.com.paper.core.RemoteStore

class PrefsStore(ctx: Context) : LocalStore {
    private val sp = ctx.getSharedPreferences("madagsha", Context.MODE_PRIVATE)
    override fun write(key: String, value: String) { sp.edit().putString(key, value).apply() }
    override fun read(key: String): String? = sp.getString(key, null)
}

/** واجهة Start.io. المعرّف يأتي من gradle.properties (STARTIO_APP_ID)؛ فارغ = الإعلانات معطلة. تحقق من أسماء الفئات مع إصدار SDK الذي تثبّته. */
class StartIoBackend(private val activity: Activity) : AdBackend {
    override fun init(): Boolean {
        if (BuildConfig.STARTIO_APP_ID.isBlank()) return false
        StartAppSDK.init(activity, BuildConfig.STARTIO_APP_ID, false)
        StartAppSDK.setTestAdsEnabled(BuildConfig.DEBUG) // وضع الاختبار في نسخ debug
        StartAppAd.disableSplash()
        return true
    }
    override fun showInterstitial(): Boolean { StartAppAd.showAd(activity); return true }
}

/** Firebase اختياري: يعمل فقط إذا وُجد google-services.json. القواعد في firestore.rules. */
class FirebaseRemote(private val ctx: Context) : RemoteStore {
    private val available get() = FirebaseApp.getApps(ctx).isNotEmpty()
    fun uid(): String? = if (available) FirebaseAuth.getInstance().currentUser?.uid else null
    override fun push(uid: String, json: String) {
        if (!available) return
        FirebaseFirestore.getInstance().collection("users").document(uid)
            .collection("matches").document("current").set(mapOf("data" to json, "updated" to System.currentTimeMillis()))
    }
}
