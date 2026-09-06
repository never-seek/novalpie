package com.novalpie.nativeapp.feature.profile

data class BlockedUser(val id:Long,val name:String,val avatarUrl:String?=null)
data class BlockedUsersPage(val users:List<BlockedUser>,val page:Int,val total:Int,val pages:Int)
