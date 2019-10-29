package org.mbari.vars.annotation.model.simple

import java.util.UUID

import com.google.gson.annotations.Expose

/**
 * @author Brian Schlining
 * @since 2019-10-28T16:57:00
 */
class DeleteCount {
  @Expose(serialize = true)
  var videoReferenceUuid: UUID = _

  @Expose(serialize = true)
  var ancillaryDataCount: Int = 0

  @Expose(serialize = true)
  var imageReferenceCount: Int = 0

  @Expose(serialize = true)
  var associationCount: Int = 0

  @Expose(serialize = true)
  var observationCount: Int = 0

  @Expose(serialize = true)
  var imagedMomentCount: Int = 0

  @Expose(serialize = true)
  var errorMessage: String = _
}

object DeleteCount {
  def apply(videoReferenceUuid: UUID,
            imagedMomentCount: Int,
            imageReferenceCount: Int,
            observationCount: Int,
            associationCount: Int,
            ancillaryDataCount: Int): DeleteCount = {
    val d = new DeleteCount
    d.videoReferenceUuid = videoReferenceUuid
    d.imagedMomentCount = imagedMomentCount
    d.imageReferenceCount = imageReferenceCount
    d.observationCount = observationCount
    d.associationCount = associationCount
    d.ancillaryDataCount = ancillaryDataCount
    d
  }

  def apply(videoReferenceUuid: UUID): DeleteCount = {
    val d = new DeleteCount
    d.videoReferenceUuid = videoReferenceUuid
    d
  }
}
