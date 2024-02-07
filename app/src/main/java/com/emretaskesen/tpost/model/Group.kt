package com.emretaskesen.tpost.model

class Group {
    var groupID:String? = null
    var groupMembers: ArrayList<String>? = null
    var groupName:String? = null
    var groupPhoto: String? = null
    constructor()
    constructor(
        groupID:String?,
        groupMembers: ArrayList<String>?,
        groupName:String?,
        groupPhoto: String?
    ){
        this.groupID = groupID
        this.groupMembers = groupMembers
        this.groupName = groupName
        this.groupPhoto = groupPhoto
    }

}