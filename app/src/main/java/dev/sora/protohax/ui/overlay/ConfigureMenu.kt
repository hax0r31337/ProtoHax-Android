package dev.sora.protohax.ui.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import dev.sora.relay.utils.logInfo

class ConfigureMenu(private val layoutWindow: LayoutWindow) {

	val hasDisplay: Boolean
		get() = menuLayout != null

	private var menuLayout: View? = null
	private val lifecycleOwner = MyLifecycleOwner()
	private val viewModelStore = ViewModelStore()

	init {
		lifecycleOwner.performRestore(null)
	}

	fun display(wm: WindowManager, ctx: Context) {
		val params = WindowManager.LayoutParams(
			WindowManager.LayoutParams.WRAP_CONTENT,
			WindowManager.LayoutParams.WRAP_CONTENT,
			WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
			WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
			PixelFormat.TRANSLUCENT
		)
		params.gravity = Gravity.TOP or Gravity.END

		val composeView = ComposeView(ctx)
		composeView.setContent {
			Text(
				text = "Hello",
				color = Color.Black,
				fontSize = 50.sp,
//				modifier = Modifier
//					.wrapContentSize()
//					.background(Color.Green)
			)
		}

		// Trick The ComposeView into thinking we are tracking lifecycle
		lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
		composeView.setViewTreeLifecycleOwner(lifecycleOwner)
		composeView.setViewTreeViewModelStoreOwner(object : ViewModelStoreOwner {
			override val viewModelStore: ViewModelStore
				get() = this@ConfigureMenu.viewModelStore
		})
		composeView.setViewTreeSavedStateRegistryOwner(lifecycleOwner)

		wm.addView(composeView, params)
		menuLayout = composeView
	}

	fun destroy(wm: WindowManager) {
		if (menuLayout == null) return
		wm.removeView(menuLayout)
		menuLayout = null
	}
}
