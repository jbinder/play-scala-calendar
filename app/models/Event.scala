package models

import org.joda.time.DateTime

case class Event(id: Option[Long], title: String, slug: String, description: String, startsAt: DateTime, endsAt: DateTime, createdAt: DateTime, locationId: Long)
