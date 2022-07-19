package com.kunminx.architecture.ui.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.core.view.children
import com.kunminx.architecture.R
import kotlin.math.abs
import kotlin.math.max

/**
 * Created by zhangxutong .
 * Date: 16/04/24
 */
class SwipeMenuLayout @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {
  private var scaleTouchSlop = 0
  private var maxVelocity = 0
  private var pointerId = 0
  private var rightMenuWidths = 0
  private var limit = 0
  private var contentView: View? = null
  private val lastP = PointF()
  private var isUnMoved = true
  private val firstP = PointF()
  private var isUserSwiped = false
  private var velocityTracker: VelocityTracker? = null
  private var expandAnim: ValueAnimator? = null
  private var closeAnim: ValueAnimator? = null
  private var isExpand = false
  private var iosInterceptFlag = false

  var isSwipeEnable = false
  var isIos = false
    private set
  var isLeftSwipe = false
    private set

  init {
    init(context, attrs, defStyleAttr)
  }

  fun setIos(ios: Boolean): SwipeMenuLayout {
    isIos = ios
    return this
  }

  fun setLeftSwipe(leftSwipe: Boolean): SwipeMenuLayout {
    isLeftSwipe = leftSwipe
    return this
  }

  private fun init(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
    scaleTouchSlop = ViewConfiguration.get(context).scaledTouchSlop
    maxVelocity = ViewConfiguration.get(context).scaledMaximumFlingVelocity
    isSwipeEnable = true
    isIos = true
    isLeftSwipe = true
    val ta = context.theme.obtainStyledAttributes(
      attrs, R.styleable.SwipeMenuLayout, defStyleAttr, 0
    )
    for (i in 0 until ta.indexCount) {
      when (val attr = ta.getIndex(i)) {
        R.styleable.SwipeMenuLayout_swipeEnable -> {
          isSwipeEnable = ta.getBoolean(attr, true)
        }
        R.styleable.SwipeMenuLayout_ios -> {
          isIos = ta.getBoolean(attr, true)
        }
        R.styleable.SwipeMenuLayout_leftSwipe -> {
          isLeftSwipe = ta.getBoolean(attr, true)
        }
      }
    }
    ta.recycle()
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    isClickable = true
    rightMenuWidths = 0
    var height = 0
    var contentWidth = 0
    val measureMatchParentChildren = MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.EXACTLY
    var isNeedMeasureChildHeight = false
    children.forEachIndexed { i, childView ->
      childView.isClickable = true
      if (childView.visibility != GONE) {
        measureChild(childView, widthMeasureSpec, heightMeasureSpec)
        val lp = childView.layoutParams as MarginLayoutParams
        height = max(height, childView.measuredHeight)
        if (measureMatchParentChildren && lp.height == LayoutParams.MATCH_PARENT) {
          isNeedMeasureChildHeight = true
        }
        if (i > 0) {
          rightMenuWidths += childView.measuredWidth
        } else {
          contentView = childView
          contentWidth = childView.measuredWidth
        }
      }
    }
    setMeasuredDimension(
      paddingLeft + paddingRight + contentWidth,
      height + paddingTop + paddingBottom
    )
    limit = rightMenuWidths * 4 / 10
    if (isNeedMeasureChildHeight) {
      forceUniformHeight(widthMeasureSpec)
    }
  }

  override fun generateLayoutParams(attrs: AttributeSet): LayoutParams {
    return MarginLayoutParams(context, attrs)
  }

  private fun forceUniformHeight(widthMeasureSpec: Int) {
    val uniformMeasureSpec = MeasureSpec.makeMeasureSpec(measuredHeight, MeasureSpec.EXACTLY)
    children.forEach { child ->
      if (child.visibility != GONE) {
        val lp = child.layoutParams as MarginLayoutParams
        if (lp.height == LayoutParams.MATCH_PARENT) {
          val oldWidth = lp.width
          lp.width = child.measuredWidth
          measureChildWithMargins(child, widthMeasureSpec, 0, uniformMeasureSpec, 0)
          lp.width = oldWidth
        }
      }
    }
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    var left = paddingLeft
    var right = paddingLeft

    children.forEachIndexed { i, childView ->
      if (childView.visibility != GONE) {
        if (i == 0) {
          childView.layout(
            left,
            paddingTop,
            left + childView.measuredWidth,
            paddingTop + childView.measuredHeight
          )
          left += childView.measuredWidth
        } else {
          if (isLeftSwipe) {
            childView.layout(
              left,
              paddingTop,
              left + childView.measuredWidth,
              paddingTop + childView.measuredHeight
            )
            left += childView.measuredWidth
          } else {
            childView.layout(
              right - childView.measuredWidth,
              paddingTop,
              right,
              paddingTop + childView.measuredHeight
            )
            right -= childView.measuredWidth
          }
        }
      }
    }
  }

  override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
    if (isSwipeEnable) {
      acquireVelocityTracker(ev)
      val verTracker = velocityTracker
      when (ev.action) {
        MotionEvent.ACTION_DOWN -> {
          isUserSwiped = false
          isUnMoved = true
          iosInterceptFlag = false
          if (isTouching) return false else isTouching = true
          lastP[ev.rawX] = ev.rawY
          firstP[ev.rawX] = ev.rawY
          if (viewCache != null) {
            if (viewCache !== this) {
              viewCache!!.smoothClose()
              iosInterceptFlag = isIos
            }
            parent.requestDisallowInterceptTouchEvent(true)
          }
          pointerId = ev.getPointerId(0)
        }
        MotionEvent.ACTION_MOVE -> {
          if (!iosInterceptFlag) {
            val gap = lastP.x - ev.rawX
            if (abs(gap) > 10 || abs(scrollX) > 10) {
              parent.requestDisallowInterceptTouchEvent(true)
            }
            if (abs(gap) > scaleTouchSlop) isUnMoved = false
            scrollBy(gap.toInt(), 0)
            if (isLeftSwipe) {
              if (scrollX < 0) scrollTo(0, 0)
              if (scrollX > rightMenuWidths) scrollTo(rightMenuWidths, 0)
            } else {
              if (scrollX < -rightMenuWidths) scrollTo(-rightMenuWidths, 0)
              if (scrollX > 0) scrollTo(0, 0)
            }
            lastP[ev.rawX] = ev.rawY
          }
        }
        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
          if (abs(ev.rawX - firstP.x) > scaleTouchSlop) {
            isUserSwiped = true
          }
          if (!iosInterceptFlag) {
            verTracker!!.computeCurrentVelocity(1000, maxVelocity.toFloat())
            val velocityX = verTracker.getXVelocity(pointerId)
            if (abs(velocityX) > 1000) {
              if (velocityX < -1000) {
                if (isLeftSwipe) smoothExpand() else smoothClose()
              } else {
                if (isLeftSwipe) smoothClose() else smoothExpand()
              }
            } else {
              if (abs(scrollX) > limit) smoothExpand() else smoothClose()
            }
          }
          releaseVelocityTracker()
          isTouching = false
        }
        else -> {}
      }
    }
    return super.dispatchTouchEvent(ev)
  }

  override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
    if (isSwipeEnable) {
      when (ev.action) {
        MotionEvent.ACTION_MOVE -> if (abs(ev.rawX - firstP.x) > scaleTouchSlop) return true
        MotionEvent.ACTION_UP -> {
          if (isLeftSwipe) {
            if (scrollX > scaleTouchSlop) {
              if (ev.x < width - scrollX) {
                if (isUnMoved) smoothClose()
                return true
              }
            }
          } else {
            if (-scrollX > scaleTouchSlop) {
              if (ev.x > -scrollX) {
                if (isUnMoved) smoothClose()
                return true
              }
            }
          }
          if (isUserSwiped) return true
        }
      }
      if (iosInterceptFlag) {
        return true
      }
    }
    return super.onInterceptTouchEvent(ev)
  }

  fun smoothExpand() {
    viewCache = this@SwipeMenuLayout
    contentView?.isLongClickable = false
    cancelAnim()
    expandAnim =
      ValueAnimator.ofInt(scrollX, if (isLeftSwipe) rightMenuWidths else -rightMenuWidths)
    expandAnim?.run {
      addUpdateListener { animation: ValueAnimator ->
        scrollTo((animation.animatedValue as Int), 0)
      }
      interpolator = OvershootInterpolator()
      addListener(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator) {
          isExpand = true
        }
      })
      setDuration(300).start()
    }
  }

  private fun cancelAnim() {
    if (closeAnim?.isRunning == true) closeAnim?.cancel()
    if (expandAnim?.isRunning == true) expandAnim?.cancel()
  }

  private fun smoothClose() {
    viewCache = null
    contentView?.isLongClickable = true
    cancelAnim()
    closeAnim = ValueAnimator.ofInt(scrollX, 0)
    closeAnim?.run {
      addUpdateListener { animation: ValueAnimator ->
        scrollTo((animation.animatedValue as Int), 0)
      }
      interpolator = AccelerateInterpolator()
      addListener(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator) {
          isExpand = false
        }
      })
      setDuration(300).start()
    }
  }

  private fun acquireVelocityTracker(event: MotionEvent) {
    if (velocityTracker == null) velocityTracker = VelocityTracker.obtain().apply {
      addMovement(event)
    }
  }

  private fun releaseVelocityTracker() {
    velocityTracker?.let {
      it.clear()
      it.recycle()
    }
    velocityTracker = null
  }

  override fun onDetachedFromWindow() {
    if (this === viewCache) {
      viewCache!!.smoothClose()
      viewCache = null
    }
    super.onDetachedFromWindow()
  }

  override fun performLongClick(): Boolean {
    return if (abs(scrollX) > scaleTouchSlop) {
      false
    } else super.performLongClick()
  }

  fun quickClose() {
    if (this === viewCache) {
      cancelAnim()
      viewCache!!.scrollTo(0, 0)
      viewCache = null
    }
  }

  companion object {
    private const val TAG = "zxt/SwipeMenuLayout"

    @SuppressLint("StaticFieldLeak")
    var viewCache: SwipeMenuLayout? = null
      private set
    private var isTouching = false
  }
}
