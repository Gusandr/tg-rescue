package me.gusandr

import app.cash.sqldelight.Transacter
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import kotlin.Unit
import me.gusandr.sqldelight.hockey.`data`.UsersQueries
import me.gusandr.tgrescue.newInstance
import me.gusandr.tgrescue.schema

public interface Users : Transacter {
  public val usersQueries: UsersQueries

  public companion object {
    public val Schema: SqlSchema<QueryResult.Value<Unit>>
      get() = Users::class.schema

    public operator fun invoke(driver: SqlDriver): Users = Users::class.newInstance(driver)
  }
}
