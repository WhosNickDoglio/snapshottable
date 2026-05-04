// MODULE: lib
// FILE: cursor_query.kt
package foo.lib

interface CursorQueryShape {
    val page: Int
}

data class Q(override val page: Int) : CursorQueryShape

// MODULE: app(lib)
// FILE: app.kt
package foo.app

import com.tunjid.snapshottable.SnapshotSpec
import com.tunjid.snapshottable.Snapshottable
import foo.lib.CursorQueryShape
import foo.lib.Q

@Snapshottable
sealed interface TilingShape<Query : CursorQueryShape, Item> {
    val currentQuery: Query
    val items: List<Item>

    @SnapshotSpec
    data class Immutable<Query : CursorQueryShape, Item>(
        override val currentQuery: Query,
        override val items: List<Item>,
    ) : TilingShape<Query, Item>
}

fun box(): String {
    val state = TilingShape.Immutable(
        currentQuery = Q(0),
        items = listOf("a"),
    ).toSnapshotMutable()

    if (state.currentQuery.page != 0) return "Fail 1"
    if (state.items != listOf("a")) return "Fail 2"

    state.update(currentQuery = Q(1))
    if (state.currentQuery.page != 1) return "Fail 3"

    val spec = state.toSnapshotSpec()
    if (spec.currentQuery.page != 1) return "Fail 4"
    if (spec.items != listOf("a")) return "Fail 5"

    return "OK"
}
