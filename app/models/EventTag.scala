package models

case class EventTag(eventId: Long, tagId: Long) {
  def patch(eventId: Option[Long], tagId: Option[Long]) = {
    this.copy(eventId.getOrElse(this.eventId), tagId.getOrElse(this.tagId))
  }
}
