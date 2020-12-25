package org.leafygreens.knut.generated.contracts

import java.io.File
import java.math.BigDecimal
import java.math.BigInteger
import java.util.UUID
import org.web3j.crypto.Credentials
import org.web3j.crypto.WalletUtils
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.request.Transaction
import org.web3j.protocol.core.methods.response.TransactionReceipt
import org.web3j.tx.Transfer
import org.web3j.tx.gas.DefaultGasProvider
import org.web3j.utils.Convert

object TestUtils {

  private val papaAccount = Credentials.create("0x8f2a55949038a9610f50fb23b5883af3b4ecb3c3bb792cbcefbd1542c692be63")

  /**
   * Generates credentials with no funds attached
   */
  fun generateCreds(): Credentials {
    val password = UUID.randomUUID().toString()
    val walletDirectory = createTempDir()
    val wallet = WalletUtils.generateNewWalletFile(password, walletDirectory)
    return WalletUtils.loadCredentials(password, File("$walletDirectory/$wallet"))
  }

  /**
   * Generates credentials that have a requested amount in ETH
   */
  fun Web3j.generateFundedCreds(amount: BigDecimal = BigDecimal.ONE): Credentials {
    val creds = generateCreds()
    fundAccount(creds.address, amount)
    return creds
  }

  /**
   * Amount in ETHER to send to account
   */
  private fun Web3j.fundAccount(address: String, amount: BigDecimal) {
    Transfer.sendFunds(this, papaAccount, address, amount, Convert.Unit.ETHER).send()
  }

  /**
   * Calculates the most recent nonce for a requested address
   */
  private fun Web3j.getLatestNonce(address: String): BigInteger = ethGetTransactionCount(address, DefaultBlockParameterName.LATEST).send().transactionCount

  /**
   * Executes a transaction and waits for an on chain receipt
   */
  fun Web3j.executeTransaction(tx: Transaction): TransactionReceipt {
    val txResponse = ethSendTransaction(tx).send()
    val txHash = txResponse.transactionHash

    do {
      // TODO Might not be strictly necessary
      Thread.sleep(100)
    } while (ethGetTransactionReceipt(txHash).send().transactionReceipt.isEmpty)

    return ethGetTransactionReceipt(txHash).send().transactionReceipt.get()
  }

  /**
   * Creates a transaction with some amount of ETH attached
   */
  fun Web3j.createTx(sender: String, contractAddress: String, encodedFunction: String, amount: BigDecimal) = createTx(sender, contractAddress, encodedFunction, amount.toBigInteger())

  /**
   * Creates a transaction optionally with no ETH attached
   */
  fun Web3j.createTx(sender: String, contractAddress: String, encodedFunction: String, amount: BigInteger? = null): Transaction = Transaction.createFunctionCallTransaction(
      sender,
      getLatestNonce(sender),
      DefaultGasProvider.GAS_PRICE,
      DefaultGasProvider.GAS_LIMIT,
      contractAddress,
      amount,
      encodedFunction
  )

}