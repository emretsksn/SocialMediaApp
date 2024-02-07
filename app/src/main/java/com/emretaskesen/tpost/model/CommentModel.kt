package com.emretaskesen.tpost.model

import com.google.firebase.Timestamp

class CommentModel (
    var userID : String?,
    var commentID: String?,
    var postID: String?,
    var commentContent : String?,
    var commentTime: Timestamp?
)