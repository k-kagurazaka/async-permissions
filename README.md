# AsyncPermissions

Easy handling for Android-M permission based on async/await


## Download

```groovy
repositories {
    maven { url "https://jitpack.io" }
}

dependencies {
    compile 'com.github.k-kagurazaka:async-permissions:0.0.1-alpha'
}
```


## Usage

Create `AsyncPermissions` instance and request permission(s) in the `UI` coroutine scope.

```kotlin
// in Activity

override fun onCreate() {
    ...
    val permissions = AsyncPermissions(this)
    // Kick a new coroutine since AsyncPermissions.request is a suspending function
    launch (UI) {
        permissions.request(Manifest.permission.CAMERA)
                .let { handlePermissionResult(it) }
    }
}

suspend fun handlePermissionResult(result: PermissionResult) {
    when (result) {
        is PermissionResult.Granted -> {
            // when the permission is granted
        }
        is PermissionResult.Denied -> {
            // when the permission is denied
        }
        is PermissionResult.NeverAskAgain -> {
            // when the permission is denied with "never ask again" check
        }
        is PermissionResult.ShouldShowRationale -> {
            // when you should show rationale of acquiring the permission
            showRationale(result)
        }
    }
}
```

You can continue permission acquiring process after showing rationale:

```kotlin
fun showRationale(result: PermissionResult.ShouldShowRationale) {
    AlertDialog.Builder(this)
            .setPositiveButton("Allow") { _, _ ->
                launch(UI) { result.proceed().let { handlePermissionResult(it) } }
            }
            .setNegativeButton("Deny") { _, _ ->
                launch(UI) { result.cancel().let { handlePermissionResult(it) } }
            }
            .setCancelable(false)
            .setMessage("You should explain why your app requests the permissions.")
            .show()
}
```


## License

    The MIT License (MIT)

    Copyright (c) 2017 Keita Kagurazaka

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in all
    copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
    SOFTWARE.
