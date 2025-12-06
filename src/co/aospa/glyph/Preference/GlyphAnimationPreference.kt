/*
 * Copyright (C) 2023-2024 Paranoid Android
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package co.aospa.glyph.Preference

import android.app.Activity
import java.io.InputStreamReader

class GlyphAnimationPreference : Preference {
    private val TAG = "GlyphAnimationPreference"
    private val DEBUG = true

    private var mActivity: Activity? = null

    private var animationName: String? = null
    private var animationTerminated = false
    private var animationPaused = true
    private var animationTimeBetween = 0
    private var animationSlugs: Array<String?>
    private var animationImgs: Array<ImageView?>

    private var mRootView: View? = null
    private val mClickListener: View.OnClickListener = View.OnClickListener { v -> performClick(v) }

    constructor(context: Context) : super(context) {
        setActivity(context)
        setLayout(R.layout.glyph_settings_preview)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        setActivity(context)
        setLayout(R.layout.glyph_settings_preview)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        setActivity(context)
        setLayout(R.layout.glyph_settings_preview)
    }

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr) {
        setActivity(context)
        setLayout(defStyleRes)
    }

    private fun setLayout(layoutResource: Int) {
        setLayoutResource(R.layout.glyph_settings_preview_frame)
        mRootView = LayoutInflater.from(getContext())
            .inflate(layoutResource, null, false)
        setShouldDisableView(false)
    }

    private fun setActivity(context: Context) {
        if (context is ContextWrapper) {
            if (context is Activity) {
                mActivity = context as Activity
            } else {
                setActivity((context as ContextWrapper).getBaseContext())
            }
        }
    }

    public override fun onBindViewHolder(holder: PreferenceViewHolder) {
        holder.itemView.setOnClickListener(mClickListener)

        val selectable: Boolean = isSelectable()
        holder.itemView.setFocusable(isSelectable())
        holder.itemView.setClickable(isSelectable())

        val layout: FrameLayout = holder.itemView as FrameLayout
        layout.removeAllViews()
        val parent: ViewGroup? = mRootView.getParent() as ViewGroup?
        if (parent != null) {
            parent.removeView(mRootView)
        }
        layout.addView(mRootView)
    }

    public override fun onAttached() {
        super.onAttached()
        if (DEBUG) Log.d(TAG, "onAttached")
        startAnimation()
    }

    public override fun onDetached() {
        super.onDetached()
        if (DEBUG) Log.d(TAG, "onDetached")
        stopAnimation()
    }

    private fun startAnimation() {
        animationSlugs = ResourceUtils.getStringArray("glyph_settings_animations_slugs")
        animationImgs = kotlin.arrayOfNulls<ImageView>(animationSlugs.size)
        for (i in animationSlugs.indices) {
            animationImgs[i] = mRootView.findViewById(
                ResourceUtils.getIdentifier("preview_device_" + animationSlugs[i], "id")
            ) as ImageView?
        }
        animationThread.start()
    }

    private fun stopAnimation() {
        animationTerminated = true
        animationThread.interrupt()
    }

    @JvmOverloads
    fun updateAnimation(play: Boolean, name: String? = animationName, time: Int = 0) {
        animationTimeBetween = time
        animationName = name
        animationPaused = !play
        animationThread.interrupt()
    }

    var animationThread: Thread = object : Thread() {
        override fun run() {
            while (!animationTerminated) {
                while (animationPaused) {
                }
                if (DEBUG) Log.d(TAG, "Displaying animation | name: " + animationName)
                try {
                    BufferedReader(
                        InputStreamReader(
                            ResourceUtils.getAnimation(animationName)
                        )
                    ).use { reader ->
                        var line: String?
                        while ((reader.readLine().also { line = it }) != null) {
                            line = line!!.replace(" ", "")
                            line =
                                if (line.endsWith(",")) line.substring(0, line.length - 1) else line
                            val split: Array<String?> =
                                line.split(",".toRegex()).dropLastWhile { it.isEmpty() }
                                    .toTypedArray()
                            if (co.aospa.glyph.Constants.Constants.getDevice() == "phone1" && split.size == 5) { // Phone (1) pattern on Phone (1)
                                mActivity.runOnUiThread({
                                    for (i in animationSlugs.indices) {
                                        setGlyphsDrawable(animationImgs[i], split[i]!!.toInt())
                                    }
                                })
                            } else if (co.aospa.glyph.Constants.Constants.getDevice() == "phone2" && split.size == 5) { // Phone (1) pattern on Phone (2)
                                mActivity.runOnUiThread({
                                    setGlyphsDrawable(animationImgs[0], split[0]!!.toInt())
                                    setGlyphsDrawable(animationImgs[1], split[0]!!.toInt())
                                    setGlyphsDrawable(animationImgs[2], split[1]!!.toInt())
                                    setGlyphsDrawable(animationImgs[3], split[2]!!.toInt())
                                    setGlyphsDrawable(animationImgs[4], split[2]!!.toInt())
                                    setGlyphsDrawable(animationImgs[5], split[2]!!.toInt())
                                    setGlyphsDrawable(animationImgs[6], split[2]!!.toInt())
                                    setGlyphsDrawable(animationImgs[7], split[2]!!.toInt())
                                    setGlyphsDrawable(animationImgs[8], split[2]!!.toInt())
                                    setGlyphsDrawable(animationImgs[9], split[3]!!.toInt())
                                    setGlyphsDrawable(animationImgs[10], split[4]!!.toInt())
                                })
                            } else if (co.aospa.glyph.Constants.Constants.getDevice() == "phone2" && split.size == 33) { // Phone (2) pattern on Phone (2)
                                mActivity.runOnUiThread({
                                    setGlyphsDrawable(animationImgs[0], split[0]!!.toInt())
                                    setGlyphsDrawable(animationImgs[1], split[1]!!.toInt())
                                    setGlyphsDrawable(animationImgs[2], split[2]!!.toInt())
                                    setGlyphsDrawable(animationImgs[3], split[3]!!.toInt())
                                    setGlyphsDrawable(animationImgs[4], split[19]!!.toInt())
                                    setGlyphsDrawable(animationImgs[5], split[20]!!.toInt())
                                    setGlyphsDrawable(animationImgs[6], split[21]!!.toInt())
                                    setGlyphsDrawable(animationImgs[7], split[22]!!.toInt())
                                    setGlyphsDrawable(animationImgs[8], split[23]!!.toInt())
                                    setGlyphsDrawable(animationImgs[9], split[25]!!.toInt())
                                    setGlyphsDrawable(animationImgs[10], split[24]!!.toInt())
                                })
                            } else if (co.aospa.glyph.Constants.Constants.getDevice() == "phone2a" && split.size == 26) { // Phone (2a) pattern on Phone (2a)
                                mActivity.runOnUiThread({
                                    setGlyphsDrawable(animationImgs[0], split[0]!!.toInt())
                                    setGlyphsDrawable(animationImgs[1], split[25]!!.toInt())
                                    setGlyphsDrawable(animationImgs[2], split[24]!!.toInt())
                                })
                            } else {
                                if (DEBUG) Log.d(
                                    TAG,
                                    "Animation line length mismatch | name: " + animationName + " | line: " + line
                                )
                                updateAnimation(false)
                            }
                            sleep(16, 666000)
                        }
                        sleep(animationTimeBetween.toLong())
                    }
                } catch (e: Exception) {
                    if (DEBUG) Log.d(
                        TAG,
                        "Exception while displaying animation | name: " + animationName + " | exception: " + e
                    )
                } finally {
                    if (animationPaused) {
                        if (DEBUG) Log.d(TAG, "Pause displaying animation | name: " + animationName)
                        mActivity.runOnUiThread({
                            for (i in animationSlugs.indices) {
                                setGlyphsDrawable(animationImgs[i], 0)
                            }
                        })
                    }
                }
            }
        }

        private fun setGlyphsDrawable(imageView: ImageView, brightness: Int) {
            if (brightness <= 0) {
                imageView.setAlpha(0.3f)
            } else {
                val brightnessFactor =
                    (0.4 + 0.6 * (brightness / co.aospa.glyph.Constants.Constants.getMaxBrightness()
                        .toDouble())).toFloat()
                imageView.setAlpha(brightnessFactor)
            }
        }
    }
}
