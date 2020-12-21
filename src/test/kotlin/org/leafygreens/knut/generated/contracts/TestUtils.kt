package org.leafygreens.knut.generated.contracts

import java.io.File
import java.math.BigDecimal
import java.util.UUID
import org.web3j.crypto.Credentials
import org.web3j.crypto.WalletUtils
import org.web3j.protocol.Web3j
import org.web3j.tx.Transfer
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
  fun Web3j.generateFundedCreds(amount: BigDecimal): Credentials {
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

}