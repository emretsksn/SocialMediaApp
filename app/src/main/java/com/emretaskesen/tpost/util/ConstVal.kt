package com.emretaskesen.tpost.util

import com.emretaskesen.tpost.BuildConfig
class ConstVal {
    object Message{
        const val KEY_TEXT_REPLY = "key_text_reply"
    }
    object Permissions {
        const val REQUEST_CODE = 100
    }

    object Configurations {
        const val GIPHY_API_KEY = BuildConfig.GIPHY_API_KEY
    }

    object Notifications {
        const val CHANNEL_ID = "SocialApp"
    }
}