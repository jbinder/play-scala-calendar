package models

case class Location(id: Option[Long], title: String, address: String, city: String, zipCode: String, state: String, country: String) {
  def patch(title: Option[String], address: Option[String], city: Option[String], zipCode: Option[String], state: Option[String], country: Option[String]) = {
    this.copy(title = title.getOrElse(this.title),
      address = address.getOrElse(this.address),
      city = city.getOrElse(this.city),
      zipCode = zipCode.getOrElse(this.zipCode),
      state = state.getOrElse(this.state),
      country = country.getOrElse(this.country)
    )
  }
}
