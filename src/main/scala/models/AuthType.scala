package models

import java.security.PublicKey

sealed trait AuthType

object AuthType {
  case class UserAuthType(userName: String)      extends AuthType
  case class DeployKeyType(publicKey: PublicKey) extends AuthType

  /**
   * Retrieves username if authType is UserAuthType, otherwise None.
   */
  def userName(authType: AuthType): Option[String] =
    authType match {
      case UserAuthType(userName) => Some(userName)
      case _                      => None
    }
}
