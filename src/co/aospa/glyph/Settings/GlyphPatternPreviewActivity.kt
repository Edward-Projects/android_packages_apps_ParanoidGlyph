/*
 * Copyright (C) 2024-2025 LunarisAOSP
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
package co.aospa.glyph.Settings

import android.app.Activity
import co.aospa.glyph.Manager.SettingsManager

class GlyphPatternPreviewActivity : Activity() {
    private var statusText: TextView? = null
    private var previewComposerButton: Button? = null
    private var previewFallbackButton: Button? = null
    private var stopButton: Button? = null

    private var currentRingtoneUri: Uri? = null
    private var composerPattern: GlyphPattern? = null
    private var hasComposerPattern = false

    private var previewHandler: Handler? = null
    private var isPreviewRunning = false
    private var fallbackThread: Thread? = null

    protected override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        previewHandler = Handler(Looper.getMainLooper())

        val scrollView: android.widget.ScrollView = createLayout()
        setContentView(scrollView)

        scrollView.setOnApplyWindowInsetsListener({ v, insets ->
            val top: Int = insets.getSystemWindowInsetTop()
            val bottom: Int = insets.getSystemWindowInsetBottom()
            v.setPadding(v.getPaddingLeft(), top, v.getPaddingRight(), bottom)
            insets.consumeSystemWindowInsets()
        })

        getWindow().getDecorView().setSystemUiVisibility(
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        )

        analyzeCurrentRingtone()
    }

    private fun createLayout(): android.widget.ScrollView {
        val scrollView: android.widget.ScrollView = ScrollView(this)
        scrollView.setFillViewport(true)

        val layout: LinearLayout = LinearLayout(this)
        layout.setOrientation(LinearLayout.VERTICAL)

        val paddingHorizontal = 50
        val paddingVertical = 50
        layout.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical)

        val title: TextView = TextView(this)
        title.setText("Glyph Pattern Preview")
        title.setTextSize(20)
        title.setTypeface(null, android.graphics.Typeface.BOLD)
        title.setPadding(0, 0, 0, 30)

        statusText = TextView(this)
        statusText.setText("Analyzing current ringtone...")
        statusText.setTextSize(14)
        statusText.setPadding(0, 0, 0, 30)
        statusText.setLineSpacing(8, 1.0f)

        previewComposerButton = Button(this)
        previewComposerButton.setText("Preview Composer Pattern")
        previewComposerButton.setEnabled(false)
        previewComposerButton.setOnClickListener({ v -> previewComposerPattern() })

        val buttonParams: LinearLayout.LayoutParams = LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        buttonParams.setMargins(0, 10, 0, 10)

        previewFallbackButton = Button(this)
        previewFallbackButton.setText("Preview Fallback Animation")
        previewFallbackButton.setEnabled(true)
        previewFallbackButton.setOnClickListener({ v -> previewFallbackAnimation() })

        stopButton = Button(this)
        stopButton.setText("Stop Preview")
        stopButton.setEnabled(false)
        stopButton.setOnClickListener({ v -> stopPreview() })

        layout.addView(title)
        layout.addView(statusText)
        layout.addView(previewComposerButton, buttonParams)
        layout.addView(previewFallbackButton, buttonParams)
        layout.addView(stopButton, buttonParams)

        scrollView.addView(layout)
        return scrollView
    }

    private fun analyzeCurrentRingtone() {
        currentRingtoneUri = RingtoneManager.getActualDefaultRingtoneUri(
            this, RingtoneManager.TYPE_RINGTONE
        )

        if (currentRingtoneUri == null) {
            statusText.setText("No ringtone set\n\nOnly fallback preview available")
            return
        }

        val audioPath = getRealPathFromUri(currentRingtoneUri)

        if (audioPath == null) {
            statusText.setText(
                "Current Ringtone: " + currentRingtoneUri.getLastPathSegment() +
                        "\n\n⚠ Cannot access file path\n\nOnly fallback preview available"
            )
            return
        }

        val status = StringBuilder()
        status.append("Current Ringtone:\n")
        status.append(audioPath.substring(audioPath.lastIndexOf('/') + 1))
        status.append("\n\n")

        val patternPath: String? = GlyphComposerParser.getGlyphPatternPath(audioPath)

        if (patternPath != null) {
            composerPattern = GlyphComposerParser.parseFromFile(patternPath)

            if (composerPattern != null && GlyphComposerParser.isValid(composerPattern)) {
                hasComposerPattern = true
                status.append("✓ Composer Pattern Found\n")
                status.append("  Frames: ").append(composerPattern.getFrames().size).append("\n")
                status.append("  Duration: ").append(composerPattern.getDuration() / 1000)
                    .append("s\n\n")
                status.append("Both preview options available:")

                previewComposerButton.setEnabled(true)
            } else {
                status.append("✗ Pattern file invalid\n\n")
                status.append("Only fallback preview available")
            }
        } else {
            status.append("✗ No Composer Pattern\n\n")
            status.append("Only fallback preview available")
        }

        statusText.setText(status.toString())
    }

    private fun previewComposerPattern() {
        if (!hasComposerPattern || composerPattern == null) {
            Toast.makeText(this, "No composer pattern available", Toast.LENGTH_SHORT).show()
            return
        }

        if (!SettingsManager.isGlyphEnabled()) {
            Toast.makeText(this, "Glyph is disabled in settings", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Playing Composer Pattern", Toast.LENGTH_SHORT).show()
        Log.d(
            TAG,
            "Previewing composer pattern with " + composerPattern.getFrames().size + " frames"
        )

        previewComposerButton.setEnabled(false)
        previewFallbackButton.setEnabled(false)
        stopButton.setEnabled(true)
        isPreviewRunning = true

        playComposerPatternPreview(composerPattern, 0, System.currentTimeMillis())
    }

    private fun playComposerPatternPreview(
        pattern: GlyphPattern,
        frameIndex: Int,
        startTime: Long
    ) {
        if (!isPreviewRunning) {
            Log.d(TAG, "Preview stopped by user")
            return
        }

        if (frameIndex >= pattern.getFrames().size) {
            Log.d(TAG, "Composer preview completed")
            runOnUiThread({
                isPreviewRunning = false
                previewComposerButton.setEnabled(true)
                previewFallbackButton.setEnabled(true)
                stopButton.setEnabled(false)
                Toast.makeText(this, "Preview completed", Toast.LENGTH_SHORT).show()
            })
            return
        }

        val frame: GlyphFrame = pattern.getFrames().get(frameIndex)
        val currentTime = System.currentTimeMillis() - startTime
        var delay: Long = frame.getTimestamp() - currentTime

        if (delay < 0) delay = 0

        previewHandler.postDelayed({
            if (!isPreviewRunning) return@postDelayed
            val brightness = scaleBrightness(frame.getBrightness())
            AnimationManager.playGlyphFrame(this, frame.getZones(), brightness, frame.getDuration())
            playComposerPatternPreview(pattern, frameIndex + 1, startTime)
        }, delay)
    }

    private fun previewFallbackAnimation() {
        if (!SettingsManager.isGlyphEnabled()) {
            Toast.makeText(this, "Glyph is disabled in settings", Toast.LENGTH_SHORT).show()
            return
        }

        val fallbackAnimation = SettingsManager.getGlyphCallAnimation()
        Toast.makeText(this, "Playing Fallback: " + fallbackAnimation, Toast.LENGTH_SHORT).show()
        Log.d(TAG, "Previewing fallback animation: " + fallbackAnimation)

        previewComposerButton.setEnabled(false)
        previewFallbackButton.setEnabled(false)
        stopButton.setEnabled(true)
        isPreviewRunning = true

        fallbackThread = Thread(Runnable {
            AnimationManager.playCsv(this, fallbackAnimation)
            if (isPreviewRunning) {
                runOnUiThread({
                    isPreviewRunning = false
                    previewComposerButton.setEnabled(hasComposerPattern)
                    previewFallbackButton.setEnabled(true)
                    stopButton.setEnabled(false)
                    Toast.makeText(this, "Preview completed", Toast.LENGTH_SHORT).show()
                })
            }
        })
        fallbackThread!!.start()
    }

    private fun stopPreview() {
        Log.d(TAG, "Stopping preview")

        isPreviewRunning = false

        if (previewHandler != null) {
            previewHandler.removeCallbacksAndMessages(null)
        }

        if (fallbackThread != null && fallbackThread!!.isAlive()) {
            fallbackThread!!.interrupt()
        }

        AnimationManager.stopAll()

        previewComposerButton.setEnabled(hasComposerPattern)
        previewFallbackButton.setEnabled(true)
        stopButton.setEnabled(false)

        Toast.makeText(this, "Preview stopped", Toast.LENGTH_SHORT).show()
    }

    private fun scaleBrightness(patternBrightness: Int): Int {
        val userBrightness = SettingsManager.getGlyphBrightness()
        return (patternBrightness * userBrightness) / 100
    }

    private fun getRealPathFromUri(uri: Uri?): String? {
        if (uri == null) return null

        if ("file" == uri.getScheme()) {
            return uri.getPath()
        }

        if ("content" == uri.getScheme()) {
            var cursor: android.database.Cursor? = null
            try {
                val projection = arrayOf<String?>(android.provider.MediaStore.Audio.Media.DATA)
                cursor = getContentResolver().query(uri, projection, null, null, null)
                if (cursor != null && cursor.moveToFirst()) {
                    val columnIndex: Int = cursor.getColumnIndexOrThrow(
                        android.provider.MediaStore.Audio.Media.DATA
                    )
                    return cursor.getString(columnIndex)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting path from URI", e)
            } finally {
                if (cursor != null) cursor.close()
            }
        }

        return null
    }

    protected override fun onDestroy() {
        stopPreview()
        super.onDestroy()
    }

    protected override fun onPause() {
        super.onPause()
        if (isPreviewRunning) {
            stopPreview()
        }
    }

    companion object {
        private const val TAG = "GlyphPatternPreview"
    }
}