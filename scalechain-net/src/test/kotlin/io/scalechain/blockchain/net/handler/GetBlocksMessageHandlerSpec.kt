package io.scalechain.blockchain.net.handler

import io.kotlintest.matchers.Matchers
import java.io.File

class GetBlocksMessageHandlerSpec : MessageHandlerTestTrait(), Matchers {

  override val testPath = File("./target/unittests-GetBlocksMessageHandlerSpec/")

  init {
    "handle" should "" {
    }
  }
}