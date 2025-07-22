package me.gusandr.sqldelight.hockey.`data`

import kotlin.Boolean
import kotlin.Long
import kotlin.String

public data class Users(
  public val userId: Long,
  public val messageIds: String?,
  public val isBanned: Boolean?,
  public val blockTime: Long?,
)
