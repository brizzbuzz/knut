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
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.request.Transaction
import org.web3j.protocol.core.methods.response.TransactionReceipt
import org.web3j.tx.TransactionManager
import org.web3j.tx.gas.ContractGasProvider
import org.web3j.tx.gas.DefaultGasProvider
import org.web3j.utils.Convert

@EVMTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GringottsTest {

  private lateinit var web3j: Web3j
  private lateinit var transactionManager: TransactionManager
  private lateinit var contractGasProvider: ContractGasProvider

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

    this.web3j = web3j
    this.transactionManager = transactionManager
    this.contractGasProvider = contractGasProvider
  }

  @Test
  internal fun `Gringotts mints knuts and creates an unbreakable vow`() {
    val creds = generateFundedCreds(BigDecimal.ONE, web3j)
    val startingBalance = web3j.ethGetBalance(creds.address, DefaultBlockParameterName.LATEST).send().balance

    val lockupAmount = Convert.toWei(BigDecimal(0.5), Convert.Unit.ETHER)
    val txReceipt = performSimpleLockup(creds, lockupAmount)

    val gasCost = txReceipt.gasUsed.times(contractGasProvider.gasPrice)

    val newBalance = startingBalance
        .minus(lockupAmount.toBigInteger())
        .minus(gasCost)

    assertEquals(BigInteger.valueOf(500), knut.balanceOf(creds.address).send())
    assertEquals(lockupAmount.toBigInteger(), vows.checkPosition(BigInteger.ONE).send().component1())
    assertEquals(BigInteger.valueOf(500), vows.checkPosition(BigInteger.ONE).send().component2())
    assertEquals(newBalance, web3j.ethGetBalance(creds.address, DefaultBlockParameterName.LATEST).send().balance)
  }

  @Test
  internal fun `Gringotts allows positions to be exercised`() {
//    TODO("Not yet implemented")
  }

  private fun performSimpleLockup(creds: Credentials, lockupAmount: BigDecimal): TransactionReceipt {
    val lockupFunction = Function(
        "lockup",
        listOf(Address(creds.address)),
        emptyList()
    )

    val encodedFunction = FunctionEncoder.encode(lockupFunction)

    val tx = Transaction.createFunctionCallTransaction(
        creds.address,
        web3j.getLatestNonce(creds.address),
        DefaultGasProvider.GAS_PRICE,
        DefaultGasProvider.GAS_LIMIT,
        gringotts.contractAddress,
        lockupAmount.toBigInteger(),
        encodedFunction
    )

    val txResponse = web3j.ethSendTransaction(tx).send()
    val txHash = txResponse.transactionHash

    do {
      Thread.sleep(100)
    } while (web3j.ethGetTransactionReceipt(txHash).send().transactionReceipt.isEmpty)

    return web3j.ethGetTransactionReceipt(txHash).send().transactionReceipt.get()
  }

  private fun Web3j.getLatestNonce(address: String): BigInteger = this.ethGetTransactionCount(address, DefaultBlockParameterName.LATEST).send().transactionCount
}