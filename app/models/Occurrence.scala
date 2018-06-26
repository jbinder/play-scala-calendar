package models

import org.joda.time.DateTime

case class Occurrence(id: Option[Long], seriesId: Long, date: DateTime, deleted: Boolean, modified: Boolean)
