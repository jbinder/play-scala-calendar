package models

import org.joda.time.DateTime

case class Event(id: Option[Long], title: String, slug: String, description: String, createdAt: DateTime, locationId: Long)
