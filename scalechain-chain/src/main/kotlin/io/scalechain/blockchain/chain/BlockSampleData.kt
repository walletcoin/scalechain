package io.scalechain.blockchain.chain

import io.scalechain.blockchain.storage.index.KeyValueDatabase
import io.scalechain.blockchain.transaction.CoinAmount
import io.scalechain.blockchain.script.hash

/**
  * Create following blocks for testing block reorganization.
  * ( Coinbase maturity for the testnet is 2. As B03a/B03b needs to spend the coins generated by B01,
  *   We need to have one more block B02 to make the coin generated by B01 mature)
  *
  * T0 :
  *   Genesis → B01(4) → B02(4) → B03a(4) : The best chain.
  *
  * T1 :
  *   Genesis → B01(4) → B02(4) → B03a(4) : (Still) the best chain.
  *                             ↘ B03b(4)
  *
  * T2 :
  *   Genesis → B01(4) → B02(4) → B03a(4) → B04a(4)
  *                             ↘ B03b(4) → B04b(8) : The best chain. (Block reorganization required)
  *
  * T3 :
  *   Genesis → B01(4) → B02(4) → B03a(4) → B04a(4) → B05a(8) : The best chain. ( Block reorganization required)
  *                             ↘ B03b(4) → B04b(8)
  *
  *
  * Transaction Dependency :
  * (Used by TransactionOrphangeSpec )
  *   TX02 → TX03
  *     ↘       ↘
  *       ↘ → → → → TX04
  */
open class BlockSampleData(val keyValueDB : KeyValueDatabase) : AbstractBlockBuildingTest() {
  override val db : KeyValueDatabase = keyValueDB

  val Addr1 = generateAccountAddress("Address1") // address 1
  val Addr2 = generateAccountAddress("Address2") // address 2
  val Addr3 = generateAccountAddress("Address3") // address 3

  inner class TxClass {
    val GEN01 = generationTransaction( "GenTx.BLK01", CoinAmount(50), Addr1.address )
    val GEN02 = generationTransaction( "GenTx.BLK02", CoinAmount(50), Addr1.address )
    val GEN03a = generationTransaction( "GenTx.BLK03", CoinAmount(50), Addr1.address )
    val GEN04a = generationTransaction( "GenTx.BLK04", CoinAmount(50), Addr1.address )
    val GEN05a = generationTransaction( "GenTx.BLK05", CoinAmount(50), Addr1.address )
    val GEN03b = generationTransaction( "GenTx.BLK03b", CoinAmount(50), Addr1.address )
    val GEN04b = generationTransaction( "GenTx.BLK04b", CoinAmount(50), Addr1.address )

    val TX02 = normalTransaction(
      "TX02",
      spendingOutputs = listOf( getOutput(GEN01,0) ),
      newOutputs = listOf(
        NewOutput(CoinAmount(10), Addr2.address),
        NewOutput(CoinAmount(18), Addr1.address),
        NewOutput(CoinAmount(10), Addr3.address),
        NewOutput(CoinAmount(11), Addr1.address)
        // We have very expensive fee, 1 SC
      )
    )
    // UTXO : TX02 : 0,1,2,3

    val TX03 = normalTransaction(
      "TX03",
      spendingOutputs = listOf( getOutput(TX02,0) ),
      newOutputs = listOf(
        NewOutput(CoinAmount(9), Addr2.address)
        // We have very expensive fee, 1 SC
      )
    )
    // UTXO : TX02 : 1,2,3
    // UTXO : TX03 : 0

    // Case 1 : Two transactions TX03a, TX03b in different block spends the same output. (conflict)
    val TX03a = normalTransaction(
      "TX03a",
      spendingOutputs = listOf( getOutput(TX02,1) ),
      newOutputs = listOf(
        NewOutput(CoinAmount(17), Addr2.address)
        // We have very expensive fee, 2 SC
      )
    )
    // UTXO : TX02 : 2,3
    // UTXO : TX03 : 0
    val TX03b = normalTransaction(
      "TX03b",
      spendingOutputs = listOf( getOutput(TX02,1) ),
      newOutputs = listOf(
        NewOutput(CoinAmount(15), Addr2.address)
        // We have very expensive fee, 4 SC
      )
    )
    // UTXO : TX02 : 2,3
    // UTXO : TX03 : 0

    val TX04 = normalTransaction(
      "TX04",
      spendingOutputs = listOf( getOutput(TX03,0), getOutput(TX02,3) ),
      newOutputs = listOf(
        NewOutput(CoinAmount(8), Addr2.address)
        // We have very expensive fee, 12) SC
      )
    )
    // UTXO : TX02 : 2
    // UTXO : TX04 : 0

    // Case 2 : Two transactions TX04a, TX04b in different block spends different outputs. (no conflict)
    // None of them goes to the transaction pool, as they spend an ouput already spent by another branch.

    val TX04a = normalTransaction(
      "TX04a",
      spendingOutputs = listOf( getOutput(TX04,0) ),
      newOutputs = listOf(
        NewOutput(CoinAmount(6), Addr2.address)
        // We have very expensive fee, 2 SC
      )
    )
    // UTXO : TX02 : 2
    // UTXO : TX04a : 0

    // TX04b can't go into the transaction pool when the BLK04a becomes the best block,
    // as it depends on the output GEN03b created on the branch b.
    val TX04b = normalTransaction(
      "TX04b",
      spendingOutputs = listOf( getOutput(GEN03b,0) ),
      newOutputs = listOf(
        NewOutput(CoinAmount(4), Addr2.address)
        // We have very expensive fee, 4 SC
      )
    )
    // UTXO : TX02 : 2
    // UTXO : TX04 : 0
    // UTXO : TX04b : 0

    // TX04b2 goes to the transaction pool, as it depends on the unpent output, (TX02,2)
    val TX04b2 = normalTransaction(
      "TX04b2",
      spendingOutputs = listOf( getOutput(TX02,2) ),
      newOutputs = listOf(
        NewOutput(CoinAmount(9), Addr2.address)
        // We have very expensive fee, 1 SC
      )
    )
    // UTXO : TX04 : 0
    // UTXO : TX04b : 0

    val TX05a = normalTransaction(
      "TX05a",
      spendingOutputs = listOf( getOutput(TX04a,0) ),
      newOutputs = listOf(
        NewOutput(CoinAmount(5), Addr2.address)
        // We have very expensive fee, 1 SC
      )
    )
    // UTXO : TX02 : 2
    // UTXO : TX05a : 0

  }
  val Tx = TxClass()

  inner class BlockClass {
    val BLK01  = doMining( newBlock(env().GenesisBlockHash,  listOf(Tx.GEN01)), 4)
    // BUGBUG : Need to spend the outputs of GEN01 after the coinbase maturity (=two confirmations in the testnet) is met.
    val BLK02  = doMining( newBlock(BLK01.header.hash(),     listOf(Tx.GEN02, Tx.TX02)), 4)

    val BLK03a = doMining( newBlock(BLK02.header.hash(),     listOf(Tx.GEN03a, Tx.TX03, Tx.TX03a)), 4)
    val BLK04a = doMining( newBlock(BLK03a.header.hash(),    listOf(Tx.GEN04a, Tx.TX04, Tx.TX04a)), 4)
    val BLK05a = doMining( newBlock(BLK04a.header.hash(),    listOf(Tx.GEN05a, Tx.TX05a)), 8)

    val BLK03b = doMining( newBlock(BLK02.header.hash(),     listOf(Tx.GEN03b, Tx.TX03, Tx.TX03b)), 4)
    val BLK04b = doMining( newBlock(BLK03b.header.hash(),    listOf(Tx.GEN04b, Tx.TX04, Tx.TX04b, Tx.TX04b2)), 8)
  }
  val Block = BlockClass()
}
