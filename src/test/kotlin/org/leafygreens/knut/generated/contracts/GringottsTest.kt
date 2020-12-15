package org.leafygreens.knut.generated.contracts

import java.math.BigDecimal
import java.math.BigInteger
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.leafygreens.knut.generated.contracts.TestUtils.generateFundedCreds
import org.web3j.EVMTest
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Function
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.methods.request.Transaction
import org.web3j.tx.TransactionManager
import org.web3j.tx.gas.ContractGasProvider
import org.web3j.tx.gas.DefaultGasProvider

@EVMTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GringottsTest {

  private lateinit var gringotts: Gringotts
  private lateinit var knut: Knut
  private lateinit var vows: UnbreakableVow

  @BeforeAll
  fun deploy(
      web3j: Web3j,
      transactionManager: TransactionManager,
      contractGasProvider: ContractGasProvider
  ) {
    gringotts = Gringotts.deploy(web3j, transactionManager, contractGasProvider).send()
    knut = Knut.load(gringotts.knut().send(), web3j, transactionManager, contractGasProvider)
    vows = UnbreakableVow.load(gringotts.vows().send(), web3j, transactionManager, contractGasProvider)
  }

  @Test
  internal fun `Gringotts mints knuts and creates an unbreakable vow`(web3j: Web3j) {
    val creds = generateFundedCreds(BigDecimal.ONE, web3j)
    val lockupFunction = Function(
        "lockup",
        listOf(Address(creds.address)),
        emptyList()
    )
    val encodedFunction = FunctionEncoder.encode(lockupFunction)
    val tx = Transaction.createFunctionCallTransaction(
        creds.address,
        BigInteger.ZERO,
        DefaultGasProvider.GAS_PRICE,
        DefaultGasProvider.GAS_LIMIT,
        gringotts.contractAddress,
        BigInteger.valueOf(10000),
        encodedFunction
    )

    assertEquals(BigInteger.ZERO, knut.balanceOf(creds.address).send())

    val txResponse = web3j.ethSendTransaction(tx).send()
    val txHash = txResponse.transactionHash

    do {
      Thread.sleep(100)
    } while (web3j.ethGetTransactionReceipt(txHash).send().transactionReceipt.isEmpty)

    assertEquals(BigInteger.valueOf(500), knut.balanceOf(creds.address).send())
    assertEquals(BigInteger.valueOf(10000), vows.checkPosition(BigInteger.ONE).send().component1())
    assertEquals(BigInteger.valueOf(500), vows.checkPosition(BigInteger.ONE).send().component2())
  }

  @Test
  internal fun `Gringotts allows positions to be exercised`() {
    TODO("Not yet implemented")
  }
}