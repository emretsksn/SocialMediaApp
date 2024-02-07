package com.emretaskesen.tpost.model

import com.google.firebase.Timestamp

class UserModel {
    var userID : String? = null
    var personalName : String? = null
    var userName : String? = null
    private var userMail : String? = null
    var biography : String? = null
    var profileImage : String? = null
    private var joinTime : Timestamp? = null
    var userGender: String? = null
    var userLink: String? =null
    var birthday: String? = null

    constructor()
    constructor(
        userID : String? ,
        personalName : String? ,
        userName : String? ,
        userMail : String? ,
        biography : String? ,
        profileImage : String? ,
        joinTime : Timestamp? ,
        userGender: String?,
        userLink: String?,
        birthday: String?
    ) {
        this.userID = userID
        this.personalName = personalName
        this.userName = userName
        this.userMail = userMail
        this.biography = biography
        this.profileImage = profileImage
        this.joinTime = joinTime
        this.userGender = userGender
        this.userLink = userLink
        this.birthday = birthday
    }

}