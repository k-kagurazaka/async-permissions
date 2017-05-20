package com.kkagurazaka.asyncpermissions

import android.annotation.TargetApi
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.PermissionChecker
import android.support.v7.app.AppCompatActivity
import kotlinx.coroutines.experimental.CancellableContinuation

class AsyncPermissionsFragment : Fragment() {

    private lateinit var queue: PermissionsContinuationQueue

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        queue = PermissionsContinuationQueue()
    }

    @TargetApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQUEST_CODE) return

        val cont = queue.poll(permissions) ?: return

        if (grantResults.all { it.isGranted }) {
            PermissionResult.Granted(permissions.toList()).let(cont::resume)
        } else {
            if (shouldShowRequestPermissionsRationale(permissions)) {
                PermissionResult.Denied(permissions.toList()).let(cont::resume)
            } else {
                PermissionResult.NeverAskAgain(permissions.toList()).let(cont::resume)
            }
        }
    }

    internal fun request(
            vararg permissions: String,
            cont: CancellableContinuation<PermissionResult>
    ) {
        if (checkSelfPermissions(context, permissions)) {
            PermissionResult.Granted(permissions.toList()).let(cont::resume)
            return
        }

        if (shouldShowRequestPermissionsRationale(permissions)) {
            PermissionResult.ShouldShowRationale(permissions.toList(), this).let(cont::resume)
        } else {
            queue.offer(permissions, cont)
            requestPermissions(permissions, REQUEST_CODE)
        }
    }

    internal fun requestFromRationale(
            vararg permissions: String,
            cont: CancellableContinuation<PermissionResult>
    ) {
        queue.offer(permissions, cont)
        requestPermissions(permissions, REQUEST_CODE)
    }

    private fun shouldShowRequestPermissionsRationale(permissions: Array<out String>): Boolean =
            permissions.asSequence().any(this::shouldShowRequestPermissionRationale)

    companion object {

        private const val TAG: String = "com.kkagurazaka.asyncpermissions.AsyncPermissionsFragment"

        private const val REQUEST_CODE = 33

        fun attach(activity: AppCompatActivity): AsyncPermissionsFragment {
            val fragmentManager = activity.supportFragmentManager

            val fragment = (fragmentManager.findFragmentByTag(TAG) as? AsyncPermissionsFragment?)
            if (fragment != null) return fragment

            return AsyncPermissionsFragment().also {
                fragmentManager.apply {
                    beginTransaction()
                            .add(it, TAG)
                            .commitAllowingStateLoss()
                    executePendingTransactions()
                }
            }
        }

        private fun checkSelfPermissions(context: Context, permissions: Array<out String>): Boolean =
                permissions.asSequence().all { checkSelfPermission(context, it) }

        private fun checkSelfPermission(context: Context, permission: String): Boolean =
                try {
                    PermissionChecker.checkSelfPermission(context, permission).isGranted
                } catch (e: Exception) {
                    false
                }

        private val Int.isGranted: Boolean
            get() = (this == PackageManager.PERMISSION_GRANTED)
    }

    private class PermissionsContinuationQueue {

        private val queue = mutableListOf<Entry>()

        fun offer(permissions: Array<out String>, cont: CancellableContinuation<PermissionResult>) {
            Entry(permissions.asKey(), cont).let(queue::add)
        }

        fun poll(permissions: Array<out String>): CancellableContinuation<PermissionResult>? {
            val key = permissions.asKey()
            return queue.find { it.key == key }?.let {
                queue.remove(it)
                it.cont
            }
        }

        private data class Entry(
                val key: String,
                val cont: CancellableContinuation<PermissionResult>
        )

        private fun Array<out String>.asKey(): String = this.joinToString(":")
    }
}
