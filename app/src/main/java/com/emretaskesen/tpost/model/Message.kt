package com.emretaskesen.tpost.model

import com.google.firebase.Timestamp
import kotlin.properties.Delegates

class Message{
    var messageID : String? = null
    var senderID : String? = null
    var messageTime : Timestamp? = null
    var messageContent : String? = null
    var messageImage : String? = null
    var messageGif : String? = null
    var readStatus : Boolean? = null
    var messageLocation: Map<String,Double>? = null
    var locationName: String? = null

    constructor()
    constructor(
        messageID : String?,
        senderID : String?,
        messageTime : Timestamp?,
        messageContent : String?,
        messageImage : String?,
        messageGif : String?,
        readStatus : Boolean?,
        messageLocation: Map<String,Double>?,
        locationName: String?
        ){
        this.messageID = messageID
        this.senderID = senderID
        this.messageTime = messageTime
        this.messageContent = messageContent
        this.messageImage = messageImage
        this.messageGif = messageGif
        this.readStatus = readStatus
        this.messageLocation = messageLocation
        this.locationName = locationName
    }
}