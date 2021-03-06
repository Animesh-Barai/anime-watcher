package brunodles.animewatcher.cast

import android.content.Context
import com.google.android.gms.cast.CastMediaControlIntent
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider

@Suppress("unused") // This class is used on `AndroidManifest.xml`
internal class CastOptionsProvider : OptionsProvider {

    override fun getCastOptions(appContext: Context): CastOptions? {
        val castOptions = CastOptions.Builder()
                .setReceiverApplicationId(CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID)
                .build()
        return castOptions
    }

    override fun getAdditionalSessionProviders(context: Context): List<SessionProvider>? {
        return null
    }
}