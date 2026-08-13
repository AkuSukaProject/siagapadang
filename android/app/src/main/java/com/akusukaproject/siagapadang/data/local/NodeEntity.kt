package com.akusukaproject.siagapadang.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tb_nodes")
data class NodeEntity(
    @PrimaryKey
    @ColumnInfo(name = "node_id")
    val nodeId: Long?,
    val lat: Double?,
    val lon: Double?,
    @ColumnInfo(name = "is_safe", defaultValue = "0")
    val isSafe: Int?,
)
