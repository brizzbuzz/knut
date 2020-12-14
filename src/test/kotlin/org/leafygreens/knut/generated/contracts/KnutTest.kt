package org.leafygreens.knut.generated.contracts

import java.io.File
import java.math.BigInteger
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.web3j.EVMTest
import org.web3j.crypto.WalletUtils
import org.web3j.protocol.Web3j
import org.web3j.tx.TransactionManager
import org.web3j.tx.gas.ContractGasProvider

@EVMTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KnutTest {

  private lateinit var contract: Knut

  @BeforeAll
  fun deploy(
      web3j: Web3j,
      transactionManager: TransactionManager,
      contractGasProvider: ContractGasProvider
  ) {
    contract = Knut.deploy(web3j, transactionManager, contractGasProvider).send()
  }

  @Test
  internal fun `Contract has the correct name and symbol`() {
    assertEquals("Knut", contract.name().send())
    assertEquals("KNUT", contract.symbol().send())
  }

  @Test
  internal fun `Contract can mint new tokens to a provided address`() {
    // when
    val password = UUID.randomUUID().toString()
    val walletDirectory = createTempDir()
    val wallet = WalletUtils.generateNewWalletFile(password, walletDirectory)
    val credentials = WalletUtils.loadCredentials(password, File("$walletDirectory/$wallet"))
    val address = credentials.address
    val amount = BigInteger.valueOf(10000)

    // do
    contract.mint(address, amount).send()

    // expect
    assertEquals(amount, contract.balanceOf(address).send())
  }

  @Test
  internal fun `Contract can burn a subset of tokens`() {
    // when
    val password = UUID.randomUUID().toString()
    val walletDirectory = createTempDir()
    val wallet = WalletUtils.generateNewWalletFile(password, walletDirectory)
    val credentials = WalletUtils.loadCredentials(password, File("$walletDirectory/$wallet"))
    val address = credentials.address
    val amount = BigInteger.valueOf(10000)
    contract.mint(address, amount).send()

    // do
    contract.burn(address, amount.divide(BigInteger.TWO)).send()

    // expect
    assertEquals(amount.divide(BigInteger.TWO), contract.balanceOf(address).send())
  }
}