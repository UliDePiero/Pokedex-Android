package com.github.utn.frba.mobile.dextracker.db.table

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class SessionTable(
    val dexToken: String,
    @PrimaryKey val userId: String,
)
