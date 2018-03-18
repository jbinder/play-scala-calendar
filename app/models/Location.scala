package models

case class Location(id: Option[Long], title: String, address: String, city: String, zipCode: String, state: String, country: String)
