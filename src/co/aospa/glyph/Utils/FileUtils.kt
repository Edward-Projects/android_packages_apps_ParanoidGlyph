/*
 * Copyright (C) 2022-2024 Paranoid Android
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
package co.aospa.glyph.Utils

import android.util.Log

object FileUtils {
    private const val TAG = "GlyphFileUtils"
    private const val DEBUG = true

    fun readLine(fileName: String): String? {
        var line: String? = null
        var reader: BufferedReader? = null
        try {
            reader = BufferedReader(FileReader(fileName), 512)
            line = reader.readLine()
        } catch (e: FileNotFoundException) {
            Log.w(TAG, "No such file " + fileName + " for reading", e)
        } catch (e: IOException) {
            Log.e(TAG, "Could not read from file " + fileName, e)
        } finally {
            try {
                if (reader != null) {
                    reader.close()
                }
            } catch (e: IOException) {
            }
        }
        return line
    }

    fun readLineInt(fileName: String): Int {
        val line = readLine(fileName)
        if (line == null) {
            return 0
        }
        try {
            return line.replace("0x", "").toInt()
        } catch (e: NumberFormatException) {
            Log.e(TAG, "Could not convert string to int from file " + fileName, e)
        }
        return 0
    }

    fun writeLine(fileName: String, value: String) {
        val modePath = ResourceUtils.getString("glyph_settings_paths_mode_absolute")
        var writerMode: BufferedWriter? = null
        var writerValue: BufferedWriter? = null
        try {
            if (!modePath.isBlank()) {
                writerMode = BufferedWriter(FileWriter(modePath))
                writerMode.write("1")
            }
            writerValue = BufferedWriter(FileWriter(fileName))
            writerValue.write(value)
        } catch (e: FileNotFoundException) {
            Log.w(TAG, "No such file " + fileName + " for writing", e)
        } catch (e: IOException) {
            Log.e(TAG, "Could not write to file " + fileName, e)
        } finally {
            try {
                if (writerMode != null) {
                    writerMode.close()
                }
                if (writerValue != null) {
                    writerValue.close()
                }
            } catch (e: IOException) {
                // Ignored, not much we can do anyway
            }
        }
    }

    fun writeLine(fileName: String, value: Int) {
        writeLine(fileName, value.toString())
    }

    fun writeLine(fileName: String, value: Float) {
        writeLine(fileName, value.toString())
    }

    fun writeAllLed(value: String) {
        writeLine(ResourceUtils.getString("glyph_settings_paths_all_absolute"), value)
    }

    fun writeAllLed(value: Int) {
        writeAllLed(value.toString())
    }

    fun writeAllLed(value: Float) {
        writeAllLed(Math.round(value).toString())
    }

    fun writeFrameLed(value: String) {
        writeLine(ResourceUtils.getString("glyph_settings_paths_frame_absolute"), value)
    }

    fun writeFrameLed(value: IntArray?) {
        writeFrameLed(value.contentToString().replace("\\[|\\]".toRegex(), "").replace(", ", " "))
    }

    fun writeFrameLed(value: FloatArray) {
        val intValue = IntArray(value.size)
        for (i in value.indices) {
            intValue[i] = Math.round(value[i])
        }
        writeFrameLed(intValue)
    }

    fun writeSingleLed(led: String?, value: String?) {
        writeLine(
            ResourceUtils.getString("glyph_settings_paths_single_absolute"),
            led + " " + value
        )
    }

    fun writeSingleLed(led: Int, value: String?) {
        writeSingleLed(led.toString(), value)
    }

    fun writeSingleLed(led: String?, value: Int) {
        writeSingleLed(led, value.toString())
    }

    fun writeSingleLed(led: String?, value: Float) {
        writeSingleLed(led, Math.round(value).toString())
    }

    fun writeSingleLed(led: Int, value: Float) {
        writeSingleLed(led.toString(), Math.round(value).toString())
    }
}
