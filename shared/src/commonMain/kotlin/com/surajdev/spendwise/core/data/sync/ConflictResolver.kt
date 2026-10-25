package com.surajdev.spendwise.core.data.sync

/** The fields conflict resolution looks at, for one version of a record. */
data class VersionStamp(
    val updatedAt: Long,
    val deleted: Boolean,
    val modifiedBy: String,
)

/**
 * Last-write-wins per record.
 *
 * 1. The newer `updatedAt` wins.
 * 2. On an exact tie, a deletion beats an edit.
 * 3. Otherwise the larger device ID wins, so every device picks the same version.
 *
 * Identical versions (e.g. our own write echoed back by the server) are left alone.
 */
object ConflictResolver {

    fun shouldApplyRemote(local: VersionStamp?, remote: VersionStamp): Boolean {
        if (local == null) return true
        if (local == remote) return false
        if (remote.updatedAt != local.updatedAt) return remote.updatedAt > local.updatedAt
        if (remote.deleted != local.deleted) return remote.deleted
        return remote.modifiedBy > local.modifiedBy
    }
}
