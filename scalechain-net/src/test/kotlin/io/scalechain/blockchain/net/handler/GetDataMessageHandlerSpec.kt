package io.scalechain.blockchain.net.handler

import io.kotlintest.matchers.Matchers
import java.io.File

class GetDataMessageHandlerSpec : MessageHandlerTestTrait(), Matchers {

  override val testPath = File("./target/unittests-GetDataMessageHandlerSpec/")

  init {
    "handle" should "" {
    }
  }
}