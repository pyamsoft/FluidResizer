/*
 * Copyright (C) 2018 POP Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.popinnow.android.fluidresizer.internal

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.view.View
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.Event.ON_DESTROY
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.popinnow.android.fluidresizer.KeyboardVisibilityChanged
import com.popinnow.android.fluidresizer.internal.KeyboardVisibilityDetector.Listener

internal data class FluidResizeListener internal constructor(
  private val activity: Activity,
  private val lifecycle: Lifecycle
) : LifecycleObserver {

  private var heightAnimator: ValueAnimator? = null
  private var resizeListener: Listener? = null

  init {
    lifecycle.addObserver(this)
  }

  internal inline fun beginListening(crossinline onChange: (event: KeyboardVisibilityChanged) -> Unit) {
    val viewHolder =
      ActivityViewHolder.createFrom(activity)

    val listener =
      KeyboardVisibilityDetector.listen(viewHolder) {
        animateHeight(viewHolder, it)
        onChange(it)
      }

    viewHolder.onDetach {
      destroyAnimator()
    }

    resizeListener = listener
  }

  @Suppress("unused")
  @OnLifecycleEvent(ON_DESTROY)
  internal fun stopListening() {
    lifecycle.removeObserver(this)

    resizeListener?.stopListening()
    destroyAnimator()
  }

  private fun destroyAnimator() {
    heightAnimator?.cancel()
    heightAnimator = null
  }

  private fun animateHeight(
    viewHolder: ActivityViewHolder,
    event: KeyboardVisibilityChanged
  ) {
    val contentView = viewHolder.contentView
    contentView.setHeight(event.contentHeightBeforeResize)

    destroyAnimator()

    // Warning: animating height might not be very performant. Try turning on
    // "Profile GPI rendering" in developer options and check if the bars stay
    // under 16ms in your app. Using Transitions API would be more efficient, but
    // for some reason it skips the first animation and I cannot figure out why.
    val animator = ObjectAnimator.ofInt(event.contentHeightBeforeResize, event.contentHeight)
        .apply {
          interpolator = FastOutSlowInInterpolator()
          duration = 300
        }
    animator.addUpdateListener { contentView.setHeight(it.animatedValue as Int) }
    animator.start()
    heightAnimator = animator
  }

  private fun View.setHeight(height: Int) {
    val params = layoutParams
    params.height = height
    layoutParams = params
  }

}