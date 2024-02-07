package com.emretaskesen.tpost.model

import com.google.firebase.Timestamp


class Post{
   var userID : String? = null
   var postID : String? = null
   var postContent : String? = null
   var postDate : Timestamp? = null
   var taggedUserIDs: ArrayList<String>? = null
   var postImages: ArrayList<String>? = null
   var postLocation: Map<String,Any>? = null
   var locationName: String? = null
   constructor()
   constructor(
      userID: String?,
      postID :String?,
      postContent: String?,
      postDate: Timestamp?,
      taggedUserIDs: ArrayList<String>?,
      postImages: ArrayList<String>?,
      postLocation: Map<String,Any>?,
      locationName: String?
   ){
      this.userID = userID
      this.postID = postID
      this.postContent = postContent
      this.postDate = postDate
      this.taggedUserIDs = taggedUserIDs
      this.postImages = postImages
      this.postLocation = postLocation
      this.locationName = locationName
   }
}