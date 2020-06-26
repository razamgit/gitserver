package models

sealed trait AccessLevel {
  def role: Integer

  case object OwnerAccess extends AccessLevel {
    val role = 0
  }

  case object EditAccess extends AccessLevel {
    val role = 20
  }

  case object ViewAccess extends AccessLevel {
    val role = 30
  }
}
