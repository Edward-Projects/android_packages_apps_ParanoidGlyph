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
package co.aospa.glyph.Composer

import android.content.Context
import java.io.InputStreamReader

object GlyphComposerParser {
    private const val TAG = "GlyphComposerParser"
    private const val DEBUG = true

    fun parseFromFile(filePath: String?): GlyphPattern? {
        if (filePath == null || filePath.isEmpty()) {
            if (DEBUG) Log.e(TAG, "Invalid file path")
            return null
        }

        val file: File = File(filePath)
        if (!file.exists() || !file.canRead()) {
            if (DEBUG) Log.e(TAG, "File does not exist or cannot be read: " + filePath)
            return null
        }

        try {
            BufferedReader(FileReader(file)).use { reader ->
                val json = StringBuilder()
                var line: String?
                while ((reader.readLine().also { line = it }) != null) {
                    json.append(line)
                }

                val pattern = parseJson(json.toString())
                if (DEBUG) Log.d(TAG, "Successfully parsed pattern from: " + filePath)
                return pattern
            }
        } catch (e: IOException) {
            if (DEBUG) Log.e(TAG, "Error reading file: " + filePath, e)
            return null
        } catch (e: JSONException) {
            if (DEBUG) Log.e(TAG, "Invalid JSON format in: " + filePath, e)
            return null
        }
    }

    fun parseFromUri(context: Context, uri: Uri?): GlyphPattern? {
        if (uri == null) {
            if (DEBUG) Log.e(TAG, "Invalid URI")
            return null
        }

        try {
            context.getContentResolver().openInputStream(uri).use { inputStream ->
                BufferedReader(
                    InputStreamReader(inputStream)
                ).use { reader ->
                    val json = StringBuilder()
                    var line: String?
                    while ((reader.readLine().also { line = it }) != null) {
                        json.append(line)
                    }

                    val pattern = parseJson(json.toString())
                    if (DEBUG) Log.d(TAG, "Successfully parsed pattern from URI: " + uri)
                    return pattern
                }
            }
        } catch (e: IOException) {
            if (DEBUG) Log.e(TAG, "Error reading URI: " + uri, e)
            return null
        } catch (e: JSONException) {
            if (DEBUG) Log.e(TAG, "Invalid JSON format from URI: " + uri, e)
            return null
        }
    }

    @Throws(JSONException::class)
    private fun parseJson(jsonString: String?): GlyphPattern {
        val json: JSONObject = JSONObject(jsonString)

        val pattern = GlyphPattern()
        pattern.setVersion(json.optInt("version", 1))
        pattern.setAudioFile(json.optString("audio_file", ""))
        pattern.setDuration(json.optLong("duration", 0))

        val framesArray: JSONArray = json.getJSONArray("frames")
        val frames: MutableList<GlyphFrame?> = ArrayList<GlyphFrame?>()

        for (i in 0..<framesArray.length()) {
            val frameJson: JSONObject = framesArray.getJSONObject(i)
            val frame: GlyphFrame = GlyphFrame()

            frame.setTimestamp(frameJson.getLong("timestamp"))
            frame.setBrightness(frameJson.getInt("brightness"))
            frame.setDuration(frameJson.getInt("duration"))


            // Parse zones array
            val zonesArray: JSONArray = frameJson.getJSONArray("zones")
            val zones = IntArray(zonesArray.length())
            for (j in 0..<zonesArray.length()) {
                zones[j] = zonesArray.getInt(j)
            }
            frame.setZones(zones)

            frames.add(frame)
        }

        pattern.setFrames(frames)
        return pattern
    }

    fun getGlyphPatternPath(audioPath: String?): String? {
        if (audioPath == null) return null

        var basePath: String? = audioPath
        val lastDot = audioPath.lastIndexOf('.')
        if (lastDot > 0) {
            basePath = audioPath.substring(0, lastDot)
        }

        val glyphPath = basePath + ".glyphring"
        val glyphFile: File = File(glyphPath)

        if (glyphFile.exists() && glyphFile.canRead()) {
            if (DEBUG) Log.d(TAG, "Found Glyph pattern file: " + glyphPath)
            return glyphPath
        }

        if (DEBUG) Log.d(TAG, "No Glyph pattern found for: " + audioPath)
        return null
    }

    fun isValid(pattern: GlyphPattern?): Boolean {
        if (pattern == null) return false
        if (pattern.getFrames() == null || pattern.getFrames().isEmpty()) return false
        if (pattern.getDuration() <= 0) return false

        for (frame in pattern.getFrames()) {
            if (frame.getZones() == null || frame.getZones().size == 0) return false
            if (frame.getBrightness() < 0 || frame.getBrightness() > 4095) return false
            if (frame.getTimestamp() < 0) return false
        }

        return true
    }
}