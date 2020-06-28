package models

sealed trait AccessLevel {
  def role: Integer
}

case object OwnerAccess extends AccessLevel {
  val role = 0
}

case object EditAccess extends AccessLevel {
  val role = 20
}

case object ViewAccess extends AccessLevel {
  val role = 30
}

object AccessLevel {
  def fromRole(role: Int): Option[AccessLevel] =
    role match {
      case _ if role == OwnerAccess.role => Some(OwnerAccess)
      case _ if role == EditAccess.role  => Some(EditAccess)
      case _                             => Option.empty[AccessLevel]
    }

}

sealed trait Account {
  def accountId: Long
  def access: Option[int]
}

case class AccountWPassword(accountId: Long, password: HashedString, access: Option[Int]) extends Account
case class AccountWKey(accountId: Long, key: SshKey, access: Option[Int])                 extends Account
