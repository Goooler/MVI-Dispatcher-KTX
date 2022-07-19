package com.kunminx.architecture.utils

import android.app.Application

/**
 * <pre>
 * blog  : http://blankj.com
 * time  : 16/12/08
 * desc  : utils about initialization
</pre> *
 */
object Utils {
  lateinit var app: Application
    private set

  fun init(app: Application) {
    this.app = app
  }
}
