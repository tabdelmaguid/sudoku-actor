package org.tabdelmaguid

import scala.collection.mutable

package object sudoku_actor {
  implicit class MapUtils[K, V](val mutableMap: mutable.Map[K, V]) {
    def initOrUpdate(key: K, initVal: V, updateFun: V => V): Unit = {
      mutableMap(key) = mutableMap.get(key) match {
        case None => initVal
        case Some(value) => updateFun(value)
      }
    }
  }

  def optionIf[T](cond: Boolean, value: => T): Option[T] = if (cond) Option(value) else None
}