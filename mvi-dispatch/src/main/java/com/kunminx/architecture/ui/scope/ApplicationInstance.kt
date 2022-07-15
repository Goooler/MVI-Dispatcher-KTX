package com.kunminx.architecture.ui.scope

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

/**
 * Create by KunMinX at 2022/7/6
 */
class ApplicationInstance private constructor() : ViewModelStoreOwner {
  private var mAppViewModelStore: ViewModelStore? = null
  override fun getViewModelStore(): ViewModelStore {
    if (mAppViewModelStore == null) mAppViewModelStore = ViewModelStore()
    return mAppViewModelStore!!
  }

  companion object {
    val instance = ApplicationInstance()
  }
}