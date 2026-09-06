package com.novalpie.nativeapp.ui

/** Captured at the button press, not from mutable route state inside a delayed coroutine. */
internal data class ContentMutationIdentity(val route:AppRoute,val detailRevision:Long,val environmentRevision:Long)
internal fun isCurrentContentMutation(identity:ContentMutationIdentity,route:AppRoute,detailRevision:Long,environmentRevision:Long):Boolean=
    identity.route==route&&identity.detailRevision==detailRevision&&identity.environmentRevision==environmentRevision
