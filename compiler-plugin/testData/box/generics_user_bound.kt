// MODULE: lib
// FILE: marker.kt
package foo.lib

interface Marker {
    val tag: String
}

class Tagged(override val tag: String) : Marker

// MODULE: app(lib)
// FILE: app.kt
package foo.app

import com.tunjid.snapshottable.SnapshotSpec
import com.tunjid.snapshottable.Snapshottable
import foo.lib.Marker
import foo.lib.Tagged

fun box(): String {
    val state = MarkerState.Immutable(
        value = Tagged("first"),
        label = "one",
    ).toSnapshotMutable()

    if (state.value.tag != "first") return "Fail 1"
    if (state.label != "one") return "Fail 2"

    state.value = Tagged("second")
    state.update(label = "two")
    if (state.toSnapshotSpec().value.tag != "second") return "Fail 3"
    if (state.toSnapshotSpec().label != "two") return "Fail 4"

    return "OK"
}

@Snapshottable
interface MarkerState<T : Marker> {
    val value: T
    val label: String

    @SnapshotSpec
    data class Immutable<T : Marker>(
        override val value: T,
        override val label: String = "default",
    ) : MarkerState<T>
}
