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

class GlyphPattern {
    var version: Int = 0
    var audioFile: String? = null
    var duration: Long = 0
    var frames: MutableList<GlyphFrame?>? = null

    constructor()

    constructor(
        version: Int,
        audioFile: String?,
        duration: Long,
        frames: MutableList<GlyphFrame?>?
    ) {
        this.version = version
        this.audioFile = audioFile
        this.duration = duration
        this.frames = frames
    }

    class GlyphFrame {
        var timestamp: Long = 0
        var zones: IntArray?
        var brightness: Int = 0
        var duration: Int = 0

        constructor()

        constructor(timestamp: Long, zones: IntArray?, brightness: Int, duration: Int) {
            this.timestamp = timestamp
            this.zones = zones
            this.brightness = brightness
            this.duration = duration
        }
    }
}