package com.kkagurazaka.experimental.asyncpermissions

import kotlinx.coroutines.experimental.suspendCancellableCoroutine

sealed class PermissionResult {

    val permission: String
        get() = permissions[0]

    abstract val permissions: List<String>

    data class Granted(override val permissions: List<String>) : PermissionResult()

    data class ShouldShowRationale(
            override val permissions: List<String>,
            private val fragment: AsyncPermissionsFragment
    ) : PermissionResult() {

        suspend fun proceed(): PermissionResult = suspendCancellableCoroutine { cont ->
            fragment.requestFromRationale(*permissions.toTypedArray(), cont = cont)
        }

        suspend fun cancel(): PermissionResult = suspendCancellableCoroutine { cont ->
            Denied(permissions).let(cont::resume)
        }
    }

    data class Denied(override val permissions: List<String>) : PermissionResult()

    data class NeverAskAgain(override val permissions: List<String>) : PermissionResult()
}
