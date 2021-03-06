package io.scalechain.blockchain.api.command

import io.scalechain.blockchain.api.command.blockchain.GetBlockResult
import io.scalechain.blockchain.api.command.rawtx.*
import io.scalechain.blockchain.proto.*
import io.scalechain.blockchain.proto.codec.TransactionCodec
import io.scalechain.blockchain.proto.codec.BlockCodec
import io.scalechain.blockchain.script.hash
import io.scalechain.util.HexUtil
import io.scalechain.util.toByteArray
import org.slf4j.LoggerFactory


// <API layer> Convert a block to a specific block format.
object BlockFormatter {
  /** Get the GetBlockResult data class instance from a block.
    *
    * Used by : getblock RPC.
    *
    * @param block The block to format.
    * @return The GetBlockResult instance.
    */
  fun getBlockResult(blockInfo : BlockInfo, block : Block) : GetBlockResult {
    val serializedBlock = BlockCodec.encode(block)

    val blockHash = block.header.hash()

    val txHashes = block.transactions.map { tx -> tx.hash() }

    return GetBlockResult(
      hash = blockHash,
      size = serializedBlock.size,
      height = blockInfo.height,
      version = block.header.version,
      merkleroot = Hash(block.header.hashMerkleRoot.value),
      tx = txHashes,
      time = block.header.timestamp,
      nonce = block.header.nonce,
      previousblockhash = Hash(block.header.hashPrevBlock.value)
    )
  }

  /** Get a serialized block data.
    *
    * Used by : getblock RPC.
    *
    * @param block The block to format.
    * @return The serialized string value.
    */
  fun getSerializedBlock(block : Block) : String {
    val rawBlockData : ByteArray = BlockCodec.encode(block)
    return HexUtil.hex(rawBlockData)
  }
}

// <API Layer> decode a transaction.
object TransactionDecoder {
  /** Decodes a serialized transaction hex string into a Transaction.
    *
    * Used by : decoderawtransaction RPC.
    * Used by : sendrawtransaction RPC.
    *
    * @param serializedTransaction The serialized transaction.
    * @return The decoded transaction, Transaction instance.
    */
  fun decodeTransaction(serializedTransaction : String) : Transaction {
    val rawTransaction = HexUtil.bytes(serializedTransaction)
    return TransactionCodec.decode(rawTransaction)!!
  }

  /** Decodes multiple transactions from a hex string.
    *
    * Note : This method uses parseMany, which is
    *
    * @param serializedTransactions The hex string that has multiple(or single) transactions to decode.
    * @return A list of transactions.
    */
  fun decodeTransactions(serializedTransactions : String) : List<Transaction> {
    val rawTransactions = HexUtil.bytes(serializedTransactions)

    return TransactionCodec.parseMany(rawTransactions)
  }
}

// <API Layer> encode a transaction
object TransactionEncoder {
  /** Encodes a transaction into a serialized transaction hex string.
    *
    * Used by : signrawtransaction RPC.
    *
    * @param transaction The transaction to encode.
    * @return The serialized transaction.
    */
  fun encodeTransaction(transaction : Transaction) : ByteArray {
    return TransactionCodec.encode(transaction)
  }
}


// <API Layer> decode a transaction.
object BlockDecoder {
  /** Decodes a serialized block hex string into a Block.
    *
    * Used by : submitblock RPC.
    *
    * @param serializedBlock The serialized block.
    * @return The decoded block, Block instance.
    */
  fun decodeBlock(serializedBlock : String) : Block {
    val rawBlock = HexUtil.bytes(serializedBlock)
    return BlockCodec.decode(rawBlock)!!
  }
}


// <API layer> Convert a transaction into a specific transaction format.
object TransactionFormatter {
  private val logger = LoggerFactory.getLogger(TransactionFormatter.javaClass)

  /** Get a serialized version of a transaction.
    *
    * Used by : sign raw transaction
    */
  fun getSerializedTranasction(transaction : Transaction) : String {
    val rawTransactionData : ByteArray = TransactionCodec.encode(transaction)
    return HexUtil.hex(rawTransactionData)
  }


  private fun convertTransactionInputs( inputs : List<TransactionInput> ) : List<RawTransactionInput> {
    return inputs.map { input ->
      when {
        input is NormalTransactionInput -> {
          assert( input.outputIndex < Integer.MAX_VALUE)

          RawNormalTransactionInput(
            txid      = Hash( input.outputTransactionHash.value ),
            vout      = input.outputIndex.toInt(),
            scriptSig = RawScriptSig( HexUtil.hex(input.unlockingScript.data.array) ),
            sequence  = input.sequenceNumber
          )
        }
        input is GenerationTransactionInput -> {
          RawGenerationTransactionInput(
            coinbase  = HexUtil.hex(input.coinbaseData.data.array),
            sequence  = input.sequenceNumber
          )
        }
        else -> throw AssertionError()
      }
    }
  }

  fun convertTransactionOutputs(outputs : List<TransactionOutput>) : List<RawTransactionOutput> {
    var outputIndex = -1 // Because we add 1 to the outputIndex, we set it to -1 to start from 0.
    val rawTxOutputs = outputs.map { output ->
        outputIndex += 1
        RawTransactionOutput(
          value        = java.math.BigDecimal( output.value ),
          n            = outputIndex,
          scriptPubKey = RawScriptPubKey(
            HexUtil.hex(output.lockingScript.data.array)
          )
        )
      }
    return rawTxOutputs
  }

  /** Convert a transaction to a RawTransaction instance.
    *
    * Used by getrawtransaction RPC.
    *
    * @param transaction The transaction to convert.
    * @param bestBlockHeight The height of the best block. Used for calculating block confirmations.
    * @param blockInfoOption Some(blockInfo) if the transaction is included in a block; None otherwise.
    * @return The converted RawTransaction instance.
    */
  fun getRawTransaction(transaction : Transaction, bestBlockHeight : Long, blockInfoOption : BlockInfo?) : RawTransaction {
    val serializedTransaction = getSerializedTranasction(transaction)

    val confirmations =
      if (blockInfoOption != null) {
        if (bestBlockHeight >= blockInfoOption.height) {
          // If the block is the best block, we can say, 1 confirmation.
          bestBlockHeight - blockInfoOption.height + 1L
        } else {
          logger.error("The best block height(${bestBlockHeight}) is less than the block height${blockInfoOption.height} which has a transaction.")
          assert(false)
          0L
        }
      } else {
        0L
      }

    return RawTransaction(
      hex      = serializedTransaction,
      txid     = transaction.hash(),
      version  = transaction.version,
      locktime = transaction.lockTime,
      vin      = convertTransactionInputs(transaction.inputs),
      vout     = convertTransactionOutputs(transaction.outputs),
      blockhash  = blockInfoOption?.blockHeader?.hash(),
      confirmations = confirmations,
      time     = blockInfoOption?.blockHeader?.timestamp,
      blocktime     = blockInfoOption?.blockHeader?.timestamp
    )
  }

  /** Convert a transaction to DecodedRawTransaction.
    *
    * Used by : decoderawtransaction RPC.
    *
    * @param transaction The transaction to convert.
    * @return The converted DecodedRawTransaction instance.
    */
  fun getDecodedRawTransaction(transaction : Transaction) : DecodedRawTransaction {
    return DecodedRawTransaction(
      txid     = transaction.hash(),
      version  = transaction.version,
      locktime = transaction.lockTime,
      vin      = convertTransactionInputs(transaction.inputs),
      vout     = convertTransactionOutputs(transaction.outputs)
    )
  }
}
