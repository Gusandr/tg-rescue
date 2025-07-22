package me.gusandr.tgrescue

import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import kotlin.Long
import kotlin.Unit
import kotlin.reflect.KClass
import me.gusandr.Users
import me.gusandr.sqldelight.hockey.`data`.UsersQueries

internal val KClass<Users>.schema: SqlSchema<QueryResult.Value<Unit>>
  get() = UsersImpl.Schema

internal fun KClass<Users>.newInstance(driver: SqlDriver): Users = UsersImpl(driver)

private class UsersImpl(
  driver: SqlDriver,
) : TransacterImpl(driver),
    Users {
  override val usersQueries: UsersQueries = UsersQueries(driver)

  public object Schema : SqlSchema<QueryResult.Value<Unit>> {
    override val version: Long
      get() = 1

    override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
      driver.execute(null, """
          |CREATE TABLE users (
          |  userId BIGINT PRIMARY KEY,
          |  messageIds LONGTEXT,
          |  isBanned BOOLEAN,
          |  blockTime BIGINT DEFAULT 0
          |)
          """.trimMargin(), 0)
      return QueryResult.Unit
    }

    override fun migrate(
      driver: SqlDriver,
      oldVersion: Long,
      newVersion: Long,
      vararg callbacks: AfterVersion,
    ): QueryResult.Value<Unit> = QueryResult.Unit
  }
}
