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
package co.aospa.glyph.Utils

import android.content.Context
import co.aospa.glyph.Constants.Constants
import java.io.InputStream

object ResourceUtils {
    private const val TAG = "GlyphResourceUtils"
    private const val DEBUG = true

    private var context: Context? = null
        get() {
            if (field == null) {
                field = Constants.CONTEXT
                checkNotNull(field) { "Constants.CONTEXT is not initialized" }
            }
            return field
        }
    private var assetManager: AssetManager? = null
        get() {
            if (field == null) {
                field = context.getAssets()
            }
            return field
        }
    private var resources: Resources? = null
        get() {
            if (field == null) {
                field = context.getResources()
            }
            return field
        }

    private var callAnimations: Array<String?>? = null
    private var notificationAnimations: Array<String?>? = null

    fun getIdentifier(id: String?, type: String?): Int {
        return resources.getIdentifier(id, type, context.getPackageName())
    }

    fun getBoolean(id: String?): Boolean {
        return resources.getBoolean(getIdentifier(id, "bool"))
    }

    fun getString(id: String?): String {
        return resources.getString(getIdentifier(id, "string"))
    }

    fun getInteger(id: String?): Int {
        return resources.getInteger(getIdentifier(id, "integer"))
    }

    fun getStringArray(id: String?): Array<String?> {
        return resources.getStringArray(getIdentifier(id, "array"))
    }

    fun getIntArray(id: String?): IntArray {
        return resources.getIntArray(getIdentifier(id, "array"))
    }

    fun getCallAnimations(): Array<String?>? {
        if (callAnimations == null) {
            try {
                val assets: Array<String?> = assetManager.list("call")
                for (i in assets.indices) {
                    assets[i] = assets[i]!!.replace(".csv".toRegex(), "")
                }
                callAnimations = assets
            } catch (e: IOException) {
            }
        }
        return callAnimations
    }

    fun getNotificationAnimations(): Array<String?>? {
        if (notificationAnimations == null) {
            try {
                val assets: Array<String?> = assetManager.list("notification")
                for (i in assets.indices) {
                    assets[i] = assets[i]!!.replace(".csv".toRegex(), "")
                }
                notificationAnimations = assets
            } catch (e: IOException) {
            }
        }
        return notificationAnimations
    }

    @Throws(IOException::class)
    fun getCallAnimation(name: String?): InputStream {
        if (callAnimations == null) getCallAnimations()

        if (ArrayUtils.contains(
                callAnimations,
                name
            )
        ) return assetManager.open("call/" + name + ".csv")

        return assetManager.open("call/" + getString("glyph_settings_call_animations_default") + ".csv")
    }

    @Throws(IOException::class)
    fun getNotificationAnimation(name: String?): InputStream {
        if (notificationAnimations == null) getNotificationAnimations()

        if (ArrayUtils.contains(
                notificationAnimations,
                name
            )
        ) return assetManager.open("notification/" + name + ".csv")

        return assetManager.open("call/" + getString("glyph_settings_notifs_animations_default") + ".csv")
    }

    @Throws(IOException::class)
    fun getAnimation(name: String?): InputStream {
        if (callAnimations == null) getCallAnimations()
        if (notificationAnimations == null) getNotificationAnimations()

        if (ArrayUtils.contains(callAnimations, name)) {
            return getCallAnimation(name)
        }

        if (ArrayUtils.contains(notificationAnimations, name)) {
            return getNotificationAnimation(name)
        }

        return assetManager.open(name + ".csv")
    }
}