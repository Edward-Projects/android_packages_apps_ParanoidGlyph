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

class GlyphPatternSelectorActivity : Activity() {
    private var statusText: TextView? = null
    private var currentRingtoneText: TextView? = null
    private var currentPatternText: TextView? = null
    private var patternListView: ListView? = null
    private var selectPatternButton: Button? = null

    private val patternFiles: MutableList<File> = ArrayList<File>()
    private var currentRingtoneFile: File? = null
    private var currentPatternFile: File? = null

    protected override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scrollView: ScrollView = createLayout()
        setContentView(scrollView)

        loadCurrentRingtone()
        scanPatterns()
    }

    private fun createLayout(): ScrollView {
        val scrollView: ScrollView = ScrollView(this)
        scrollView.setFillViewport(true)

        val mainLayout: LinearLayout = LinearLayout(this)
        mainLayout.setOrientation(LinearLayout.VERTICAL)
        mainLayout.setPadding(40, 100, 40, 40)

        val title: TextView = TextView(this)
        title.setText("Apply Glyph Pattern")
        title.setTextSize(22)
        title.setTypeface(null, android.graphics.Typeface.BOLD)
        title.setPadding(0, 0, 0, 20)

        statusText = TextView(this)
        statusText.setText("Select a pattern to apply to your current ringtone")
        statusText.setTextSize(12)
        statusText.setPadding(0, 0, 0, 20)

        val ringtoneLabel: TextView = TextView(this)
        ringtoneLabel.setText("Current Ringtone:")
        ringtoneLabel.setTextSize(16)
        ringtoneLabel.setTypeface(null, android.graphics.Typeface.BOLD)
        ringtoneLabel.setPadding(0, 10, 0, 5)

        currentRingtoneText = TextView(this)
        currentRingtoneText.setText("Loading...")
        currentRingtoneText.setTextSize(14)
        currentRingtoneText.setPadding(10, 5, 10, 15)

        val patternStatusLabel: TextView = TextView(this)
        patternStatusLabel.setText("Current Pattern:")
        patternStatusLabel.setTextSize(16)
        patternStatusLabel.setTypeface(null, android.graphics.Typeface.BOLD)
        patternStatusLabel.setPadding(0, 10, 0, 5)

        currentPatternText = TextView(this)
        currentPatternText.setText("None")
        currentPatternText.setTextSize(14)
        currentPatternText.setPadding(10, 5, 10, 15)

        val availableLabel: TextView = TextView(this)
        availableLabel.setText("Available Patterns:")
        availableLabel.setTextSize(16)
        availableLabel.setTypeface(null, android.graphics.Typeface.BOLD)
        availableLabel.setPadding(0, 20, 0, 10)

        patternListView = ListView(this)
        patternListView.setLayoutParams(
            LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                400
            )
        )

        selectPatternButton = Button(this)
        selectPatternButton.setText("Apply Selected Pattern")
        selectPatternButton.setEnabled(false)
        selectPatternButton.setOnClickListener({ v -> applySelectedPattern() })

        val buttonParams: LinearLayout.LayoutParams = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        buttonParams.setMargins(0, 20, 0, 10)

        val removeButton: Button = Button(this)
        removeButton.setText("Remove Current Pattern")
        removeButton.setOnClickListener({ v -> removeCurrentPattern() })

        mainLayout.addView(title)
        mainLayout.addView(statusText)
        mainLayout.addView(ringtoneLabel)
        mainLayout.addView(currentRingtoneText)
        mainLayout.addView(patternStatusLabel)
        mainLayout.addView(currentPatternText)
        mainLayout.addView(availableLabel)
        mainLayout.addView(patternListView)
        mainLayout.addView(selectPatternButton, buttonParams)
        mainLayout.addView(removeButton)

        scrollView.addView(mainLayout)
        return scrollView
    }

    private fun loadCurrentRingtone() {
        val ringtoneUri: Uri? =
            RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE)

        if (ringtoneUri == null) {
            currentRingtoneText.setText("No ringtone set")
            statusText.setText("Please set a ringtone first")
            return
        }

        val audioPath = getRealPathFromUri(ringtoneUri)

        if (audioPath == null) {
            currentRingtoneText.setText(ringtoneUri.getLastPathSegment())
            statusText.setText("⚠ Cannot access ringtone file")
            return
        }

        currentRingtoneFile = File(audioPath)
        currentRingtoneText.setText(currentRingtoneFile.getName())

        val patternPath: String? = GlyphComposerParser.getGlyphPatternPath(audioPath)
        if (patternPath != null) {
            currentPatternFile = File(patternPath)
            val appliedPatternInfo = findOriginalPatternName(currentPatternFile)
            currentPatternText.setText("✓ Applied: " + appliedPatternInfo)
            currentPatternText.setTextColor(-0xff0100)
        } else {
            currentPatternText.setText("None (using fallback animation)")
            currentPatternText.setTextColor(-0x6800)
        }
    }

    private fun findOriginalPatternName(appliedPattern: File): String {
        val savedPatternDir: File =
            File(Environment.getExternalStorageDirectory(), "Ringtones/SavedPattern")

        if (!savedPatternDir.exists()) {
            return appliedPattern.getName()
        }

        val savedPatterns: Array<File>? =
            savedPatternDir.listFiles(FilenameFilter { dir: File?, name: String? ->
                name!!.endsWith(".glyphring")
            })
        if (savedPatterns == null) {
            return appliedPattern.getName()
        }

        try {
            val appliedParsed: GlyphPattern? =
                GlyphComposerParser.parseFromFile(appliedPattern.getAbsolutePath())
            if (appliedParsed == null) return appliedPattern.getName()

            for (savedPattern in savedPatterns) {
                val savedParsed: GlyphPattern? =
                    GlyphComposerParser.parseFromFile(savedPattern.getAbsolutePath())
                if (savedParsed != null && patternsMatch(appliedParsed, savedParsed)) {
                    return savedPattern.getName()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error comparing patterns", e)
        }

        return appliedPattern.getName()
    }

    private fun patternsMatch(p1: GlyphPattern, p2: GlyphPattern): Boolean {
        if (p1.getFrames().size != p2.getFrames().size) return false
        if (p1.getDuration() != p2.getDuration()) return false
        return true
    }

    private fun scanPatterns() {
        patternFiles.clear()

        val savedPatternDir: File =
            File(Environment.getExternalStorageDirectory(), "Ringtones/SavedPattern")

        if (!savedPatternDir.exists()) {
            statusText.setText("No patterns found. Create some patterns first!")
            return
        }

        val files: Array<File>? =
            savedPatternDir.listFiles(FilenameFilter { dir: File?, name: String? ->
                name!!.endsWith(".glyphring")
            })

        if (files == null || files.size == 0) {
            statusText.setText("No patterns found in SavedPattern folder")
            return
        }

        val patternNames: MutableList<String?> = ArrayList<String?>()
        for (file in files) {
            patternFiles.add(file)

            val pattern: GlyphPattern? = GlyphComposerParser.parseFromFile(file.getAbsolutePath())
            var info: String = file.getName()
            if (pattern != null) {
                info += "\n  " + pattern.getFrames().size + " frames, " +
                        (pattern.getDuration() / 1000) + "s"
            }
            patternNames.add(info)
        }

        val adapter: ArrayAdapter<String?> = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_single_choice,
            patternNames
        )

        patternListView.setAdapter(adapter)
        patternListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE)
        patternListView.setOnItemClickListener({ parent, view, position, id ->
            selectPatternButton.setEnabled(true)
        })

        statusText.setText("Found " + patternFiles.size + " pattern(s). Select one to apply.")
    }

    private fun applySelectedPattern() {
        val selectedPosition: Int = patternListView.getCheckedItemPosition()

        if (selectedPosition == -1) {
            Toast.makeText(this, "Please select a pattern", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentRingtoneFile == null) {
            Toast.makeText(this, "No ringtone file found", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedPattern: File = patternFiles.get(selectedPosition)

        Builder(this)
            .setTitle("Apply Pattern")
            .setMessage(
                "Apply pattern '" + selectedPattern.getName() + "' to ringtone '" +
                        currentRingtoneFile.getName() + "'?"
            )
            .setPositiveButton("Apply", { dialog, which ->
                applyPattern(selectedPattern)
            })
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun applyPattern(patternFile: File) {
        try {
            val ringtoneDir: File? = currentRingtoneFile.getParentFile()

            val ringtoneName = currentRingtoneFile.getName()
            val baseName = ringtoneName.substring(0, ringtoneName.lastIndexOf('.'))
            val targetPatternName = baseName + ".glyphring"

            val targetPattern: File = File(ringtoneDir, targetPatternName)

            copyFile(patternFile, targetPattern)

            currentPatternFile = targetPattern
            currentPatternText.setText("✓ " + targetPattern.getName())
            currentPatternText.setTextColor(-0xff0100)

            Toast.makeText(this, "Pattern applied successfully!", Toast.LENGTH_LONG).show()
            Log.d(TAG, "Pattern applied: " + targetPattern.getAbsolutePath())

            Builder(this)
                .setTitle("Success!")
                .setMessage(
                    "Pattern applied to your ringtone!\n\n" +
                            "Test it by making a call or use 'Preview patterns' to see it."
                )
                .setPositiveButton("Preview", { dialog, which ->
                    val intent: Intent = Intent(this, GlyphPatternPreviewActivity::class.java)
                    startActivity(intent)
                })
                .setNegativeButton("Done", null)
                .show()
        } catch (e: Exception) {
            Log.e(TAG, "Error applying pattern", e)
            Toast.makeText(this, "Error: " + e.message, Toast.LENGTH_LONG).show()
        }
    }

    private fun removeCurrentPattern() {
        if (currentPatternFile == null || !currentPatternFile.exists()) {
            Toast.makeText(this, "No pattern to remove", Toast.LENGTH_SHORT).show()
            return
        }

        Builder(this)
            .setTitle("Remove Pattern")
            .setMessage(
                "Remove pattern from current ringtone?\n\n" +
                        "The pattern file in SavedPattern folder will NOT be deleted."
            )
            .setPositiveButton("Remove", { dialog, which ->
                if (currentPatternFile.delete()) {
                    currentPatternFile = null
                    currentPatternText.setText("None (using fallback animation)")
                    currentPatternText.setTextColor(-0x6800)
                    Toast.makeText(this, "Pattern removed", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to remove pattern", Toast.LENGTH_SHORT).show()
                }
            })
            .setNegativeButton("Cancel", null)
            .show()
    }

    @Throws(Exception::class)
    private fun copyFile(source: File, dest: File) {
        FileInputStream(source).getChannel().use { sourceChannel ->
            FileOutputStream(dest).getChannel().use { destChannel ->
                destChannel.transferFrom(sourceChannel, 0, sourceChannel.size())
            }
        }
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

    companion object {
        private const val TAG = "GlyphPatternSelector"
    }
}