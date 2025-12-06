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
import java.util.Random
import kotlin.math.max

class GlyphPatternCreatorActivity : Activity() {
    private var statusText: TextView? = null
    private var ledGrid: GridLayout? = null
    private var brightnessSeeker: SeekBar? = null
    private var durationSeeker: SeekBar? = null
    private var brightnessValue: TextView? = null
    private var durationValue: TextView? = null
    private var framesList: LinearLayout? = null
    private var addFrameButton: Button? = null
    private var previewButton: Button? = null
    private var stopPreviewButton: Button? = null
    private var saveButton: Button? = null

    private val selectedZones = BooleanArray(LED_COUNT)
    private val frames: MutableList<FrameData> = ArrayList<FrameData>()
    private var currentBrightness = 4095
    private var currentDuration = 200

    private var isPreviewRunning = false
    private var previewHandler: android.os.Handler? = null

    private inner class FrameData(
        var timestamp: Long,
        var zones: IntArray,
        var brightness: Int,
        var duration: Int
    )

    protected override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        previewHandler = Handler(getMainLooper())

        val scrollView: ScrollView = createLayout()
        setContentView(scrollView)
    }

    private fun createLayout(): ScrollView {
        val scrollView: ScrollView = ScrollView(this)
        scrollView.setFillViewport(true)

        val mainLayout: LinearLayout = LinearLayout(this)
        mainLayout.setOrientation(LinearLayout.VERTICAL)
        mainLayout.setPadding(40, 100, 40, 40)

        val title: TextView = TextView(this)
        title.setText("Glyph Pattern Creator")
        title.setTextSize(22)
        title.setTypeface(null, android.graphics.Typeface.BOLD)
        title.setGravity(Gravity.CENTER)
        title.setPadding(0, 0, 0, 20)

        statusText = TextView(this)
        statusText.setText("Select zones and add frames to create your pattern")
        statusText.setTextSize(12)
        statusText.setPadding(0, 0, 0, 20)

        val templatesLabel: TextView = TextView(this)
        templatesLabel.setText("Quick Templates (15s):")
        templatesLabel.setTextSize(16)
        templatesLabel.setTypeface(null, android.graphics.Typeface.BOLD)
        templatesLabel.setPadding(0, 10, 0, 5)

        val templatesDesc: TextView = TextView(this)
        templatesDesc.setText("Load a pre-made pattern, then customize if needed")
        templatesDesc.setTextSize(11)
        templatesDesc.setTextColor(-0x666667)
        templatesDesc.setPadding(0, 0, 0, 10)

        val templatesLayout: LinearLayout = LinearLayout(this)
        templatesLayout.setOrientation(LinearLayout.HORIZONTAL)

        val waveButton: Button = Button(this)
        waveButton.setText("Wave")
        waveButton.setLayoutParams(LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1))
        waveButton.setOnClickListener({ v -> loadTemplate("wave") })

        val pulseButton: Button = Button(this)
        pulseButton.setText("Pulse")
        pulseButton.setLayoutParams(LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1))
        pulseButton.setOnClickListener({ v -> loadTemplate("pulse") })

        val blinkButton: Button = Button(this)
        blinkButton.setText("Blink")
        blinkButton.setLayoutParams(LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1))
        blinkButton.setOnClickListener({ v -> loadTemplate("blink") })

        templatesLayout.addView(waveButton)
        templatesLayout.addView(pulseButton)
        templatesLayout.addView(blinkButton)

        val templatesLayout2: LinearLayout = LinearLayout(this)
        templatesLayout2.setOrientation(LinearLayout.HORIZONTAL)

        val breatheButton: Button = Button(this)
        breatheButton.setText("Breathe")
        breatheButton.setLayoutParams(LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1))
        breatheButton.setOnClickListener({ v -> loadTemplate("breathe") })

        val randomButton: Button = Button(this)
        randomButton.setText("Random")
        randomButton.setLayoutParams(LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1))
        randomButton.setOnClickListener({ v -> loadTemplate("random") })

        val allButton: Button = Button(this)
        allButton.setText("All On")
        allButton.setLayoutParams(LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1))
        allButton.setOnClickListener({ v -> loadTemplate("allon") })

        templatesLayout2.addView(breatheButton)
        templatesLayout2.addView(randomButton)
        templatesLayout2.addView(allButton)

        val ledLabel: TextView = TextView(this)
        ledLabel.setText("Select LED Zones:")
        ledLabel.setTextSize(16)
        ledLabel.setTypeface(null, android.graphics.Typeface.BOLD)
        ledLabel.setPadding(0, 10, 0, 10)

        ledGrid = createLedGrid()

        val clearButton: Button = Button(this)
        clearButton.setText("Clear Selection")
        clearButton.setOnClickListener({ v -> clearLedSelection() })

        val brightnessLabel: TextView = TextView(this)
        brightnessLabel.setText("Brightness:")
        brightnessLabel.setTextSize(14)
        brightnessLabel.setPadding(0, 20, 0, 5)

        val brightnessLayout: LinearLayout = LinearLayout(this)
        brightnessLayout.setOrientation(LinearLayout.HORIZONTAL)

        brightnessSeeker = SeekBar(this)
        brightnessSeeker.setMax(4095)
        brightnessSeeker.setProgress(4095)
        brightnessSeeker.setLayoutParams(LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1))
        brightnessSeeker.setOnSeekBarChangeListener(object : OnSeekBarChangeListener() {
            public override fun onProgressChanged(
                seekBar: SeekBar?,
                progress: Int,
                fromUser: Boolean
            ) {
                currentBrightness = progress
                brightnessValue.setText(progress.toString())
            }

            public override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            public override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        brightnessValue = TextView(this)
        brightnessValue.setText("4095")
        brightnessValue.setTextSize(14)
        brightnessValue.setPadding(10, 0, 0, 0)
        brightnessValue.setMinWidth(100)

        brightnessLayout.addView(brightnessSeeker)
        brightnessLayout.addView(brightnessValue)

        val durationLabel: TextView = TextView(this)
        durationLabel.setText("Duration (ms):")
        durationLabel.setTextSize(14)
        durationLabel.setPadding(0, 10, 0, 5)

        val durationLayout: LinearLayout = LinearLayout(this)
        durationLayout.setOrientation(LinearLayout.HORIZONTAL)

        durationSeeker = SeekBar(this)
        durationSeeker.setMax(1000)
        durationSeeker.setProgress(200)
        durationSeeker.setLayoutParams(LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1))
        durationSeeker.setOnSeekBarChangeListener(object : OnSeekBarChangeListener() {
            public override fun onProgressChanged(
                seekBar: SeekBar?,
                progress: Int,
                fromUser: Boolean
            ) {
                currentDuration = max(50, progress)
                durationValue.setText(currentDuration.toString())
            }

            public override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            public override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        durationValue = TextView(this)
        durationValue.setText("200")
        durationValue.setTextSize(14)
        durationValue.setPadding(10, 0, 0, 0)
        durationValue.setMinWidth(100)

        durationLayout.addView(durationSeeker)
        durationLayout.addView(durationValue)

        addFrameButton = Button(this)
        addFrameButton.setText("Add Frame")
        addFrameButton.setOnClickListener({ v -> addFrame() })
        val addFrameParams: LinearLayout.LayoutParams = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        addFrameParams.setMargins(0, 20, 0, 10)

        val framesLabel: TextView = TextView(this)
        framesLabel.setText("Timeline:")
        framesLabel.setTextSize(16)
        framesLabel.setTypeface(null, android.graphics.Typeface.BOLD)
        framesLabel.setPadding(0, 20, 0, 10)

        framesList = LinearLayout(this)
        framesList.setOrientation(LinearLayout.VERTICAL)

        val actionLayout: LinearLayout = LinearLayout(this)
        actionLayout.setOrientation(LinearLayout.HORIZONTAL)
        actionLayout.setPadding(0, 20, 0, 0)

        previewButton = Button(this)
        previewButton.setText("Preview")
        previewButton.setEnabled(false)
        previewButton.setLayoutParams(LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1))
        previewButton.setOnClickListener({ v -> previewPattern() })

        stopPreviewButton = Button(this)
        stopPreviewButton.setText("Stop")
        stopPreviewButton.setVisibility(View.GONE)
        stopPreviewButton.setLayoutParams(LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1))
        stopPreviewButton.setOnClickListener({ v -> stopPreview() })

        saveButton = Button(this)
        saveButton.setText("Save Pattern")
        saveButton.setEnabled(false)
        saveButton.setLayoutParams(LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1))
        saveButton.setOnClickListener({ v -> savePattern() })

        actionLayout.addView(previewButton)
        actionLayout.addView(stopPreviewButton)
        actionLayout.addView(saveButton)

        mainLayout.addView(title)
        mainLayout.addView(statusText)
        mainLayout.addView(templatesLabel)
        mainLayout.addView(templatesDesc)
        mainLayout.addView(templatesLayout)
        mainLayout.addView(templatesLayout2)
        mainLayout.addView(ledLabel)
        mainLayout.addView(ledGrid)
        mainLayout.addView(clearButton)
        mainLayout.addView(brightnessLabel)
        mainLayout.addView(brightnessLayout)
        mainLayout.addView(durationLabel)
        mainLayout.addView(durationLayout)
        mainLayout.addView(addFrameButton, addFrameParams)
        mainLayout.addView(framesLabel)
        mainLayout.addView(framesList)
        mainLayout.addView(actionLayout)

        scrollView.addView(mainLayout)
        return scrollView
    }

    private fun createLedGrid(): GridLayout {
        val grid: GridLayout = GridLayout(this)
        grid.setColumnCount(8)
        grid.setPadding(10, 10, 10, 10)

        for (i in 0..<LED_COUNT) {
            val zone = i

            val ledBox: CheckBox = CheckBox(this)
            ledBox.setText(i.toString())
            ledBox.setTextSize(10)
            ledBox.setPadding(5, 5, 5, 5)

            val params: GridLayout.LayoutParams = LayoutParams()
            params.width = 0
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            params.setMargins(5, 5, 5, 5)

            ledBox.setLayoutParams(params)
            ledBox.setOnCheckedChangeListener({ buttonView, isChecked ->
                selectedZones[zone] = isChecked
                updateStatus()
            })

            grid.addView(ledBox)
        }

        return grid
    }

    private fun clearLedSelection() {
        for (i in 0..<ledGrid.getChildCount()) {
            val child: View = ledGrid.getChildAt(i)
            if (child is CheckBox) {
                (child as CheckBox).setChecked(false)
            }
        }
        for (i in 0..<LED_COUNT) {
            selectedZones[i] = false
        }
        updateStatus()
    }

    private fun loadTemplate(type: String) {
        frames.clear()
        framesList.removeAllViews()
        clearLedSelection()

        when (type) {
            "wave" -> generateWaveTemplate()
            "pulse" -> generatePulseTemplate()
            "blink" -> generateBlinkTemplate()
            "breathe" -> generateBreatheTemplate()
            "random" -> generateRandomTemplate()
            "allon" -> generateAllOnTemplate()
        }

        refreshFramesList()
        updateStatus()
        previewButton.setEnabled(true)
        saveButton.setEnabled(true)

        Toast.makeText(
            this,
            "Template loaded: " + type.uppercase(Locale.getDefault()) + " (15s, " + frames.size + " frames)",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun generateWaveTemplate() {
        val frameCount = 30
        val ledsPerFrame = 4

        for (i in 0..<frameCount) {
            val timestamp = (i * 500).toLong()

            val startZone: Int = (i * ledsPerFrame) % LED_COUNT
            val zones = IntArray(ledsPerFrame)
            for (j in 0..<ledsPerFrame) {
                zones[j] = (startZone + j) % LED_COUNT
            }

            frames.add(GlyphPatternCreatorActivity.FrameData(timestamp, zones, 4095, 400))
        }
    }

    private fun generatePulseTemplate() {
        val allZones = IntArray(LED_COUNT)
        for (i in 0..<LED_COUNT) {
            allZones[i] = i
        }

        for (pulse in 0..9) {
            val baseTime = (pulse * 1500).toLong()

            for (step in 0..4) {
                val timestamp = baseTime + (step * 60)
                val brightness = 800 + (step * 650)
                frames.add(
                    GlyphPatternCreatorActivity.FrameData(
                        timestamp,
                        allZones.clone(),
                        brightness,
                        60
                    )
                )
            }

            frames.add(
                GlyphPatternCreatorActivity.FrameData(
                    baseTime + 300,
                    allZones.clone(),
                    4095,
                    400
                )
            )

            for (step in 0..4) {
                val timestamp = baseTime + 700 + (step * 60)
                val brightness = 4095 - (step * 650)
                frames.add(
                    GlyphPatternCreatorActivity.FrameData(
                        timestamp,
                        allZones.clone(),
                        brightness,
                        60
                    )
                )
            }
        }
    }

    private fun generateBlinkTemplate() {
        val zones = intArrayOf(0, 5, 10, 15, 20, 25, 30)

        for (i in 0..29) {
            val timestamp = (i * 500).toLong()

            val currentZones: IntArray?
            if (i % 2 == 0) {
                currentZones = intArrayOf(0, 2, 4, 6, 8, 10)
            } else {
                currentZones = intArrayOf(1, 3, 5, 7, 9, 11)
            }

            frames.add(GlyphPatternCreatorActivity.FrameData(timestamp, currentZones, 4095, 250))
        }
    }

    private fun generateBreatheTemplate() {
        val allZones = IntArray(LED_COUNT)
        for (i in 0..<LED_COUNT) {
            allZones[i] = i
        }

        for (cycle in 0..4) {
            val baseTime = (cycle * 3000).toLong()


            // Inhale (10 steps, 1.5s)
            for (step in 0..9) {
                val timestamp = baseTime + (step * 150)
                val brightness = 500 + (step * 360)
                frames.add(
                    GlyphPatternCreatorActivity.FrameData(
                        timestamp,
                        allZones.clone(),
                        brightness,
                        150
                    )
                )
            }

            for (step in 0..9) {
                val timestamp = baseTime + 1500 + (step * 150)
                val brightness = 4095 - (step * 360)
                frames.add(
                    GlyphPatternCreatorActivity.FrameData(
                        timestamp,
                        allZones.clone(),
                        brightness,
                        150
                    )
                )
            }
        }
    }

    private fun generateRandomTemplate() {
        val random = Random()

        for (i in 0..49) {
            val timestamp = (i * 300).toLong()

            val zoneCount = 3 + random.nextInt(6)
            val zones = IntArray(zoneCount)
            for (j in 0..<zoneCount) {
                zones[j] = random.nextInt(LED_COUNT)
            }

            val brightness = 2000 + random.nextInt(2096)

            frames.add(GlyphPatternCreatorActivity.FrameData(timestamp, zones, brightness, 250))
        }
    }

    private fun generateAllOnTemplate() {
        val allZones = IntArray(LED_COUNT)
        for (i in 0..<LED_COUNT) {
            allZones[i] = i
        }

        frames.add(GlyphPatternCreatorActivity.FrameData(0, allZones.clone(), 4095, 5000))

        frames.add(GlyphPatternCreatorActivity.FrameData(5000, allZones.clone(), 3000, 5000))

        frames.add(GlyphPatternCreatorActivity.FrameData(10000, allZones.clone(), 4095, 5000))
    }

    private fun addFrame() {
        val zonesList: MutableList<Int?> = ArrayList<Int?>()
        for (i in selectedZones.indices) {
            if (selectedZones[i]) {
                zonesList.add(i)
            }
        }

        if (zonesList.isEmpty()) {
            Toast.makeText(this, "Please select at least one zone", Toast.LENGTH_SHORT).show()
            return
        }

        val zonesArray = IntArray(zonesList.size)
        for (i in zonesList.indices) {
            zonesArray[i] = zonesList.get(i)!!
        }

        var timestamp: Long = 0
        for (frame in frames) {
            timestamp += frame.duration.toLong()
        }

        val frame: FrameData = GlyphPatternCreatorActivity.FrameData(
            timestamp,
            zonesArray,
            currentBrightness,
            currentDuration
        )
        frames.add(frame)

        addFrameToList(frame, frames.size - 1)

        updateStatus()
        previewButton.setEnabled(true)
        saveButton.setEnabled(true)

        Toast.makeText(this, "Frame added at " + timestamp + "ms", Toast.LENGTH_SHORT).show()
    }

    private fun addFrameToList(frame: FrameData, index: Int) {
        val frameLayout: LinearLayout = LinearLayout(this)
        frameLayout.setOrientation(LinearLayout.HORIZONTAL)
        frameLayout.setPadding(10, 10, 10, 10)
        frameLayout.setBackgroundResource(android.R.drawable.dialog_holo_light_frame)

        val frameParams: LinearLayout.LayoutParams = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        frameParams.setMargins(0, 5, 0, 5)

        val frameInfo: TextView = TextView(this)
        frameInfo.setText(
            String.format(
                "Frame %d: %dms\n%d zones, B:%d, D:%dms",
                index + 1, frame.timestamp, frame.zones.size, frame.brightness, frame.duration
            )
        )
        frameInfo.setTextSize(12)
        frameInfo.setLayoutParams(LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1))

        val deleteButton: Button = Button(this)
        deleteButton.setText("Delete")
        deleteButton.setOnClickListener({ v ->
            frames.removeAt(index)
            refreshFramesList()
            if (frames.isEmpty()) {
                previewButton.setEnabled(false)
                saveButton.setEnabled(false)
            }
            updateStatus()
        })

        frameLayout.addView(frameInfo)
        frameLayout.addView(deleteButton)

        framesList.addView(frameLayout, frameParams)
    }

    private fun refreshFramesList() {
        framesList.removeAllViews()
        for (i in frames.indices) {
            addFrameToList(frames.get(i), i)
        }
    }

    private fun updateStatus() {
        var selectedCount = 0
        for (selected in selectedZones) {
            if (selected) selectedCount++
        }

        var totalDuration: Long = 0
        for (frame in frames) {
            totalDuration += frame.duration.toLong()
        }

        statusText.setText(
            String.format(
                "Selected: %d zones | Frames: %d | Duration: %.1fs",
                selectedCount, frames.size, totalDuration / 1000.0
            )
        )
    }

    private fun previewPattern() {
        if (frames.isEmpty()) return

        Toast.makeText(this, "Playing preview...", Toast.LENGTH_SHORT).show()

        isPreviewRunning = true
        previewButton.setVisibility(View.GONE)
        stopPreviewButton.setVisibility(View.VISIBLE)
        saveButton.setEnabled(false)

        playPreview(0, System.currentTimeMillis())
    }

    private fun playPreview(frameIndex: Int, startTime: Long) {
        if (!isPreviewRunning) {
            onPreviewComplete()
            return
        }

        if (frameIndex >= frames.size) {
            isPreviewRunning = false
            onPreviewComplete()
            Toast.makeText(this, "Preview completed", Toast.LENGTH_SHORT).show()
            return
        }

        val frame = frames.get(frameIndex)
        val currentTime = System.currentTimeMillis() - startTime
        var delay = frame.timestamp - currentTime

        if (delay < 0) delay = 0

        previewHandler.postDelayed({
            if (!isPreviewRunning) return@postDelayed
            AnimationManager.playGlyphFrame(this, frame.zones, frame.brightness, frame.duration)
            playPreview(frameIndex + 1, startTime)
        }, delay)
    }

    private fun stopPreview() {
        if (!isPreviewRunning) return

        Log.d(TAG, "Stopping preview")
        isPreviewRunning = false

        if (previewHandler != null) {
            previewHandler.removeCallbacksAndMessages(null)
        }

        AnimationManager.stopAll()

        onPreviewComplete()
        Toast.makeText(this, "Preview stopped", Toast.LENGTH_SHORT).show()
    }

    private fun onPreviewComplete() {
        runOnUiThread({
            previewButton.setVisibility(View.VISIBLE)
            stopPreviewButton.setVisibility(View.GONE)
            saveButton.setEnabled(true)
        })
    }

    private fun savePattern() {
        if (frames.isEmpty()) {
            Toast.makeText(this, "No frames to save", Toast.LENGTH_SHORT).show()
            return
        }

        val builder: AlertDialog.Builder = Builder(this)
        builder.setTitle("Save Pattern")

        val input: EditText = EditText(this)
        input.setInputType(InputType.TYPE_CLASS_TEXT)
        input.setHint("pattern_name")
        builder.setView(input)

        builder.setPositiveButton("Save", { dialog, which ->
            var filename = input.getText().toString().trim()
            if (filename.isEmpty()) {
                Toast.makeText(this, "Please enter a filename", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            if (!filename.endsWith(".glyphring")) {
                filename += ".glyphring"
            }
            saveToFile(filename)
        })

        builder.setNegativeButton("Cancel", { dialog, which -> dialog.cancel() })
        builder.show()
    }

    private fun saveToFile(filename: String) {
        try {
            val json: JSONObject = JSONObject()
            json.put("version", 1)
            json.put("audio_file", filename.replace(".glyphring", ".ogg"))

            var totalDuration: Long = 0
            for (frame in frames) {
                totalDuration = max(totalDuration, frame.timestamp + frame.duration)
            }
            json.put("duration", totalDuration)

            val framesArray: JSONArray = JSONArray()
            for (frame in frames) {
                val frameJson: JSONObject = JSONObject()
                frameJson.put("timestamp", frame.timestamp)

                val zonesArray: JSONArray = JSONArray()
                for (zone in frame.zones) {
                    zonesArray.put(zone)
                }
                frameJson.put("zones", zonesArray)
                frameJson.put("brightness", frame.brightness)
                frameJson.put("duration", frame.duration)

                framesArray.put(frameJson)
            }
            json.put("frames", framesArray)

            val baseDir: File = File(Environment.getExternalStorageDirectory(), "Ringtones")
            val dir: File = File(baseDir, "SavedPattern")

            if (!dir.exists()) {
                val created: Boolean = dir.mkdirs()
                if (!created) {
                    Toast.makeText(this, "Failed to create directory", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Failed to create directory: " + dir.getAbsolutePath())
                    return
                }
            }

            val file: File = File(dir, filename)
            val writer: FileWriter = FileWriter(file)
            writer.write(json.toString(2))
            writer.close()

            Toast.makeText(this, "Saved to: SavedPattern/" + filename, Toast.LENGTH_LONG).show()
            Log.d(TAG, "Pattern saved: " + file.getAbsolutePath())

            showSaveSuccessDialog(file)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving pattern", e)
            Toast.makeText(this, "Error saving pattern: " + e.message, Toast.LENGTH_LONG).show()
        }
    }

    private fun showSaveSuccessDialog(savedFile: File) {
        val builder: AlertDialog.Builder = Builder(this)
        builder.setTitle("Pattern Saved!")
        builder.setMessage(
            "Pattern saved to:\n" + savedFile.getAbsolutePath() +
                    "\n\nWhat would you like to do?"
        )

        builder.setPositiveButton("Create Another", { dialog, which ->
            frames.clear()
            refreshFramesList()
            clearLedSelection()
            previewButton.setEnabled(false)
            saveButton.setEnabled(false)
            updateStatus()
            Toast.makeText(this, "Ready to create new pattern", Toast.LENGTH_SHORT).show()
        })

        builder.setNeutralButton("Done", { dialog, which ->
            finish()
        })

        builder.setNegativeButton("View File", { dialog, which ->
            Toast.makeText(this, "File: " + savedFile.getName(), Toast.LENGTH_LONG).show()
        })

        builder.show()
    }

    companion object {
        private const val TAG = "GlyphPatternCreator"
        private const val LED_COUNT = 33 // Nothing Phone 2
    }
}