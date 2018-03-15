package models

import org.joda.time.DateTime

case class Event(title: String, description: String, startsAt: DateTime, endsAt: DateTime, createdAt: DateTime)
