package org.mbari.vars.annotation.model.simple

import com.google.gson.annotations.Expose

/**
 * @author Brian Schlining
 * @since 2019-05-23T16:50:00
 */
class ErrorMsg {

  @Expose(serialize = true)
  var code: Int = _

  @Expose(serialize = true)
  var message: String = _

}

object ErrorMsg {
  def apply(code: Int, message: String): ErrorMsg = {
    val msg = new ErrorMsg
    msg.code = code
    msg.message = message
    msg
  }
}
