package com.novalpie.nativeapp.data

/** A negative server acknowledgement, distinct from an interrupted/unconfirmed write. */
class AdminActionRejectedException(message: String) : java.io.IOException(message)
