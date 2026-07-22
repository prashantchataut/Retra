package app.retra.emulator

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: RetraViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { RetraV22Root(viewModel) }
        routeExternalIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        routeExternalIntent(intent)
    }

    private fun routeExternalIntent(intent: Intent?) {
        val incoming = intent ?: return
        val uri = when (incoming.action) {
            Intent.ACTION_VIEW -> incoming.data
            Intent.ACTION_SEND -> incoming.sharedStreamUri()
            else -> null
        } ?: return
        viewModel.queueExternalImport(uri)
    }

    @Suppress("DEPRECATION")
    private fun Intent.sharedStreamUri(): Uri? {
        return clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
            ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            }
    }
}
