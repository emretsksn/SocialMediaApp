package com.emretaskesen.tpost.model

import com.google.firebase.Timestamp

class NotificationModel(
    var notificationID:String?,
    var notificationTime:Timestamp?,
    var notificationContent: String?,
    var notificationTitle:String?,
    var notificationType:String?,
    var postID: String?,
    var trackID: String?,
    var userID: String?
)