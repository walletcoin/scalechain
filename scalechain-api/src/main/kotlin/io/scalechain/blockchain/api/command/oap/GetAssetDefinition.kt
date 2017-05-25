package io.scalechain.blockchain.api.command.oap

import com.google.gson.JsonObject
import io.scalechain.blockchain.api.command.RpcCommand
import io.scalechain.blockchain.api.domain.*
import io.scalechain.blockchain.oap.OpenAssetsProtocol
import io.scalechain.util.Either
import io.scalechain.util.Either.Right
import io.scalechain.util.HexUtil

/*
*/

/** GetBalance: gets the balance in decimal bitcoins across all accounts or for a particular account.
  *
  *
  * Parameter #1 assetIdOrAddress (string, required) Asset Id or Bitcoin Address
  *
  * Result:
  * asset_ids : (string) array that containing Asset Id
  * metadata_hash : (String) Asset Definition Pointer in Hex format
  * asset_definition : (Object) Contents of asset defition file
  *
  * Json-RPC request :
  * {"jsonrpc": "1.0", "id":"curltest", "method": "getassetdefinition", "params": [ "oWW5DyHMmNpH2P9gGwDH7kLw7mgt1iV4W8"] }
  *
  * Json-RPC response :
  * {
  *    "result": {
  *        "asset_ids" : ["oWW5DyHMmNpH2P9gGwDH7kLw7mgt1iV4W8"],
  *        "metadata_hash :"f1b575b277228d97583e355951022a8ac14a12e2",
  *        "asset_definition" : {"asset_ids":["oWW5DyHMmNpH2P9gGwDH7kLw7mgt1iV4W8"],"contact_url":"https://api.scalechain.io/assets/oWW5DyHMmNpH2P9gGwDH7kLw7mgt1iV4W8","name":"OAP Test Asset 7","name_short":"TestAsset7","issuer":"ScaleChain","description":"Test Asset 7, for test purpose only.","descripton_mime":"text/plain","type":"other","divisibility":0,"link_to_website":false,"icon_url":"https://api.scalechain.io/profile/icon/oWW5DyHMmNpH2P9gGwDH7kLw7mgt1iV4W8.jpg","image_url":"https://api.scalechain.io/profile/image/oWW5DyHMmNpH2P9gGwDH7kLw7mgt1iV4W8.jpg","version":"1.0"},
  *    },
  *    "error": null,
  *    "id": "curltest"
  * }
  *
  * https://bitcoin.org/en/developer-reference#getbalance
  */
data class GetAssetDefinitionResult(val asset_ids : List<String>, val metadata_hash : String, val asset_definition : JsonObject ) : RpcResult
object GetAssetDefinition  : RpcCommand() {
  override fun invoke(request : RpcRequest) : Either<RpcError, RpcResult?> {
    return handlingException {
      val hashOrAssetId: String           = request.params.get<String>("hashOrAssetId", 0)

      val definition = OpenAssetsProtocol.get().getAssetDefinition(hashOrAssetId)

      // BUGBUG : OAP : Make sure the following field is serialized correctly : asset_definition : JsonObject

      Right(
        GetAssetDefinitionResult(
          definition.toJson().getAsJsonArray("asset_ids").map{ it.asString },
          HexUtil.hex(definition.hash()),
          definition.toJson()
        )
      )
    }
  }

  override fun help() : String =
    """getassetdefinition ( "asset_id_or_address" minconf includeWatchonly )
      |
      |If account is not specified, returns the server's total available balance.
      |If account is specified (DEPRECATED), returns the balance in the account.
      |Note that the account "" is not the same as leaving the parameter out.
      |The server total may be different to the balance in the default "" account.
      |
      |Arguments:
      |1. "asset_id_or_address"      (string) Asset Id or Bitcoi address
      |
      |Result:
      |asset_ids (string) The total amount in BTC received for this account.
      |metadata_hash (string) Asset Definition Pointer in Hex format
      |asset_definition (Object) Contents of asset defition file
      |
      |Examples:
      |
      |As a json rpc call
      |> curl --user myusername --data-binary '{"jsonrpc": "1.0", "id":"curltest", "method": "getassetdefinition", "params": [ "oWW5DyHMmNpH2P9gGwDH7kLw7mgt1iV4W8"] }' -H 'content-type: text/plain;' http://127.0.0.1:8332/
    """.trimMargin()
}
