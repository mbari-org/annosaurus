package org.mbari.vars.annotation.dao.jpa

import com.google.gson.annotations.Expose
import javax.persistence.{Column, GeneratedValue, GenerationType, Id}
import org.mbari.vars.annotation.PersistentObject


/**
  * @author Brian Schlining
  * @since 2019-02-28T10:15:00
  */
trait HasID extends PersistentObject {

  @Expose(serialize = false)
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "id", updatable = false, nullable = false)
  var id: Long = _

  override def primaryKey: Option[Long] = Option(id)
}
