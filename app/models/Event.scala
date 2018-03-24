package models

import org.joda.time.DateTime

case class Event(id: Option[Long], title: String, slug: String, description: String, startsAt: DateTime, endsAt: DateTime, createdAt: DateTime, locationId: Long) {
  def patch(title: Option[String], slug: Option[String], description: Option[String], startsAt: Option[DateTime], endsAt: Option[DateTime], locationId: Option[Long]): Event = {
    this.copy(title = title.getOrElse(this.title),
      slug = slug.getOrElse(this.slug),
      description = description.getOrElse(this.description),
      startsAt = startsAt.getOrElse(this.startsAt),
      endsAt = endsAt.getOrElse(this.endsAt),
      locationId = locationId.getOrElse(this.locationId)
    )
  }
}
