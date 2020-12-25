package org.leafygreens.knut.generated.contracts

import java.math.BigInteger
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.leafygreens.knut.generated.contracts.TestUtils.generateCreds
import org.web3j.EVMTest
import org.web3j.protocol.Web3j
import org.web3j.tx.TransactionManager
import org.web3j.tx.gas.ContractGasProvider

@EVMTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KnutTest : ContractTest() {

  private lateinit var knut: Knut

  @BeforeAll
  fun deploy(
      web3j: Web3j,
      transactionManager: TransactionManager,
      contractGasProvider: ContractGasProvider
  ) {
    this.web3j = web3j
    this.transactionManager = transactionManager
    this.contractGasProvider = contractGasProvider

    knut = Knut.deploy(web3j, transactionManager, contractGasProvider).send()
  }

  @Test
  fun `Contract has the correct name and symbol`() {
    assertEquals("Knut", knut.name().send())
    assertEquals("KNUT", knut.symbol().send())
  }

  @Test
  fun `Contract can mint new tokens to a provided address`() {
    // when
    val credentials = generateCreds()
    val address = credentials.address
    val amount = BigInteger.valueOf(10000)

    // do
    knut.mint(address, amount).send()

    // expect
    assertEquals(amount, knut.balanceOf(address).send())
  }

  @Test
  fun `Contract can burn a subset of tokens`() {
    // when
    val credentials = generateCreds()
    val address = credentials.address
    val amount = BigInteger.valueOf(10000)
    knut.mint(address, amount).send()

    // do
    knut.burn(address, amount.divide(BigInteger.TWO)).send()

    // expect
    assertEquals(amount.divide(BigInteger.TWO), knut.balanceOf(address).send())
  }

  @Test
  fun `Only contract owner can mint and burn Knuts`() {

  }
}