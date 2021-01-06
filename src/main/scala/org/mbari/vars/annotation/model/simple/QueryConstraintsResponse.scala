package org.mbari.vars.annotation.model.simple

import com.google.gson.annotations.Expose

class QueryConstraintsResponse[A] {

  @Expose(serialize = true)
  var queryConstraints: QueryConstraints = _

  @Expose(serialize = true)
  var content: A = _
}

object QueryConstraintsResponse {
  def apply[A](queryConstraints: QueryConstraints, content: A): QueryConstraintsResponse[A] = {
    val qc = new QueryConstraintsResponse[A]
    qc.queryConstraints = queryConstraints
    qc.content = content
    qc
  }
}
