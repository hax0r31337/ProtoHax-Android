package dev.sora.protohax.ui.overlay

import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner

class MyLifecycleOwner : SavedStateRegistryOwner {

	private val mSavedStateRegistryController = SavedStateRegistryController.create(this)

	override val lifecycle = LifecycleRegistry(this)
	override val savedStateRegistry: SavedStateRegistry
		get() = mSavedStateRegistryController.savedStateRegistry

	fun setCurrentState(state: Lifecycle.State) {
		lifecycle.currentState = state
	}

	fun handleLifecycleEvent(event: Lifecycle.Event) {
		lifecycle.handleLifecycleEvent(event)
	}

	fun performRestore(savedState: Bundle?) {
		mSavedStateRegistryController.performRestore(savedState)
	}

	fun performSave(outBundle: Bundle) {
		mSavedStateRegistryController.performSave(outBundle)
	}
}
