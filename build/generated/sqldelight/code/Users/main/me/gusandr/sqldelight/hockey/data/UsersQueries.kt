package me.gusandr.sqldelight.hockey.`data`

import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.JdbcCursor
import app.cash.sqldelight.driver.jdbc.JdbcPreparedStatement
import kotlin.Any
import kotlin.Boolean
import kotlin.Long
import kotlin.String

public class UsersQueries(
  driver: SqlDriver,
) : TransacterImpl(driver) {
  public fun <T : Any> selectAll(mapper: (
    userId: Long,
    messageIds: String?,
    isBanned: Boolean?,
    blockTime: Long?,
  ) -> T): Query<T> = Query(942_060_759, arrayOf("users"), driver, "Users.sq", "selectAll", """
  |SELECT users.userId, users.messageIds, users.isBanned, users.blockTime
  |FROM users
  """.trimMargin()) { cursor ->
    check(cursor is JdbcCursor)
    mapper(
      cursor.getLong(0)!!,
      cursor.getString(1),
      cursor.getLong(2)?.let { it == 1L },
      cursor.getLong(3)
    )
  }

  public fun selectAll(): Query<Users> = selectAll { userId, messageIds, isBanned, blockTime ->
    Users(
      userId,
      messageIds,
      isBanned,
      blockTime
    )
  }

  public fun <T : Any> selectMessageUser(userId: Long, mapper: (messageIds: String?) -> T): Query<T>
      = SelectMessageUserQuery(userId) { cursor ->
    check(cursor is JdbcCursor)
    mapper(
      cursor.getString(0)
    )
  }

  public fun selectMessageUser(userId: Long): Query<SelectMessageUser> = selectMessageUser(userId) {
      messageIds ->
    SelectMessageUser(
      messageIds
    )
  }

  public fun <T : Any> selectBanTime(userId: Long, mapper: (blockTime: Long?) -> T): Query<T> =
      SelectBanTimeQuery(userId) { cursor ->
    check(cursor is JdbcCursor)
    mapper(
      cursor.getLong(0)
    )
  }

  public fun selectBanTime(userId: Long): Query<SelectBanTime> = selectBanTime(userId) {
      blockTime ->
    SelectBanTime(
      blockTime
    )
  }

  /**
   * @return The number of rows updated.
   */
  public fun insert(
    userId: Long,
    messageIds: String?,
    isBanned: Boolean?,
  ): QueryResult<Long> {
    val result = driver.execute(1_661_635_111, """
        |INSERT INTO users(userId, messageIds, isBanned)
        |VALUES(?, ?, ?)
        """.trimMargin(), 3) {
          check(this is JdbcPreparedStatement)
          bindLong(0, userId)
          bindString(1, messageIds)
          bindLong(2, isBanned?.let { if (it) 1L else 0L })
        }
    notifyQueries(1_661_635_111) { emit ->
      emit("users")
    }
    return result
  }

  /**
   * @return The number of rows updated.
   */
  public fun changeMessage(messageIds: String?, userId: Long): QueryResult<Long> {
    val result = driver.execute(-1_283_875_063, """
        |UPDATE users
        |SET messageIds = ?
        |WHERE userId = ?
        """.trimMargin(), 2) {
          check(this is JdbcPreparedStatement)
          bindString(0, messageIds)
          bindLong(1, userId)
        }
    notifyQueries(-1_283_875_063) { emit ->
      emit("users")
    }
    return result
  }

  /**
   * @return The number of rows updated.
   */
  public fun changeBanStatus(
    isBanned: Boolean?,
    blockTime: Long?,
    userId: Long,
  ): QueryResult<Long> {
    val result = driver.execute(2_131_203_939, """
        |UPDATE users
        |SET isBanned = ?,
        |    blockTime = ?
        |WHERE userId = ?
        """.trimMargin(), 3) {
          check(this is JdbcPreparedStatement)
          bindLong(0, isBanned?.let { if (it) 1L else 0L })
          bindLong(1, blockTime)
          bindLong(2, userId)
        }
    notifyQueries(2_131_203_939) { emit ->
      emit("users")
    }
    return result
  }

  private inner class SelectMessageUserQuery<out T : Any>(
    public val userId: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("users", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("users", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(455_059_880, """
    |SELECT messageIds
    |FROM users
    |WHERE userId = ?
    """.trimMargin(), mapper, 1) {
      check(this is JdbcPreparedStatement)
      bindLong(0, userId)
    }

    override fun toString(): String = "Users.sq:selectMessageUser"
  }

  private inner class SelectBanTimeQuery<out T : Any>(
    public val userId: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("users", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("users", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-874_032_206, """
    |SELECT blockTime
    |FROM users
    |WHERE userId = ?
    """.trimMargin(), mapper, 1) {
      check(this is JdbcPreparedStatement)
      bindLong(0, userId)
    }

    override fun toString(): String = "Users.sq:selectBanTime"
  }
}
