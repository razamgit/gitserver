package models

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.{ Lock, ReentrantLock }

object RzRepositoryLock {
  def defining[A, B](value: A)(f: A => B): B = f(value)

  /**
   * lock objects
   */
  private val locks = new ConcurrentHashMap[String, Lock]()

  /**
   * Returns the lock object for the specified repository.
   */
  private def getLockObject(key: String): Lock = synchronized {
    if (!locks.containsKey(key)) {
      locks.put(key, new ReentrantLock())
    }
    locks.get(key)
  }

  /**
   * Synchronizes a given function which modifies the working copy of the repository.
   */
  def lock[T](key: String)(f: => T): T = defining(getLockObject(key)) { lock =>
    try {
      lock.lock()
      f
    } finally {
      lock.unlock()
    }
  }

}
