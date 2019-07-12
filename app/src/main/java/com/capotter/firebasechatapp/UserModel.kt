package com.capotter.firebasechatapp

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
 data class UserModel(val userId: String,
                      val displayName: String,
                      val email: String,
                      val userImageUrl: String,
                      var active: Boolean = false,
                      var registrationId: String = ""): Parcelable