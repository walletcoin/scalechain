package io.scalechain.blockchain.storage.index

import io.scalechain.blockchain.proto.FileNumber
import org.scalatest.Matchers

/**
  * Common util methods for testing key/value store.
  */
trait KeyValueCommonTrait : Matchers{
  /** Convert a string to a byte array.
    *
    * @param value The string to convert to a byte array.
    * @return The converted byte array.
    */
  fun B(value : String) = value.getBytes

  /** Convert an option of a byte array to an option of a byte list.
    * Because Scala uses referential equality check on arrays,
    * we need to convert arrays to lists.
    *
    * For lists, Scala checks each items in the list for the equality check.
    *
    * @param arrayOption The option of an array to convert.
    */
  fun L(arrayOption : ByteArray?) =
    arrayOption.map(_.toList)

  val PREFIX1 : Byte = '1'
  val PREFIX2 : Byte = '2'
}