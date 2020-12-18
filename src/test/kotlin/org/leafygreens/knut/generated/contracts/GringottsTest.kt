package org.leafygreens.knut.generated.contracts

import java.math.BigDecimal
import java.math.BigInteger
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.leafygreens.knut.generated.contracts.TestUtils.generateFundedCreds
import org.web3j.EVMTest
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
  internal fun `Gringotts mints knuts and creates an unbreakable vow`() = runBlocking {
    // when
    val creds = generateFundedCreds(BigDecimal.ONE, web3j)
    val startingBalance = web3j.ethGetBalance(creds.address, DefaultBlockParameterName.LATEST).send().balance
    val lockupAmount = Convert.toWei(BigDecimal(0.5), Convert.Unit.ETHER)

    // do
    val lockupReceipt = performSimpleLockup(creds, lockupAmount)
    val lockupEvent = gringotts.getLockupEvents(lockupReceipt).first()

    // expect
    val gasCost = lockupReceipt.gasUsed.times(contractGasProvider.gasPrice)

    val newBalance = startingBalance
        .minus(lockupAmount.toBigInteger())
        .minus(gasCost)

    val position = vows.checkPosition(lockupEvent.optionID).send()

    assertEquals(BigInteger.valueOf(500), knut.balanceOf(creds.address).send())
    assertEquals(lockupAmount.toBigInteger(), position.component1())
    assertEquals(BigInteger.valueOf(500), position.component2())
    assertEquals(newBalance, web3j.ethGetBalance(creds.address, DefaultBlockParameterName.LATEST).send().balance)
  }

  @Test
  internal fun `Gringotts allows multiple vows to be created`() = runBlocking {
    // when
    val creds = generateFundedCreds(BigDecimal.ONE, web3j)
    val lockupAmount = Convert.toWei(BigDecimal(0.25), Convert.Unit.ETHER)

    // do
    val lockupReceiptA = performSimpleLockup(creds, lockupAmount)
    val lockupReceiptB = performSimpleLockup(creds, lockupAmount)

    // expect
    val lockupEventA = gringotts.getLockupEvents(lockupReceiptA).first()
    val lockupEventB = gringotts.getLockupEvents(lockupReceiptB).first()

    assertEquals(lockupEventA.value.plus(lockupEventB.value), knut.balanceOf(creds.address).send())
    assertEquals(creds.address, vows.ownerOf(lockupEventA.optionID).send())
    assertEquals(creds.address, vows.ownerOf(lockupEventB.optionID).send())
  }

  @Test
  internal fun `Gringotts allows positions to be exercised`() = runBlocking {
    val creds = generateFundedCreds(BigDecimal.ONE, web3j)
    val startingBalance = web3j.ethGetBalance(creds.address, DefaultBlockParameterName.LATEST).send().balance

    val lockupAmount = Convert.toWei(BigDecimal(0.5), Convert.Unit.ETHER)
    val lockupReceipt = performSimpleLockup(creds, lockupAmount)
    val lockupEvent = gringotts.getLockupEvents(lockupReceipt)

    // do
    val exerciseReceipt = performSimpleExercise(creds, BigInteger.ONE)
    val exerciseEvent = gringotts.getExerciseEvents(exerciseReceipt)

    // expect
    assertNotNull(lockupEvent)
    assertNotNull(exerciseEvent)

    val lockupGasCost = lockupReceipt.gasUsed.times(contractGasProvider.gasPrice)
    val exerciseGasCost = exerciseReceipt.gasUsed.times(contractGasProvider.gasPrice)

    val newBalance = startingBalance
        .minus(lockupGasCost)
        .minus(exerciseGasCost)

    assertEquals(newBalance, web3j.ethGetBalance(creds.address, DefaultBlockParameterName.LATEST).send().balance)
  }

  private fun performSimpleLockup(creds: Credentials, lockupAmount: BigDecimal): TransactionReceipt {
    val lockup = gringotts.lockup().encodeFunctionCall()
    val tx = createTx(creds.address, lockup, lockupAmount.toBigInteger())
    return executeTransaction(tx)
  }

  private fun performSimpleExercise(creds: Credentials, optionId: BigInteger): TransactionReceipt {
    val encodedFunction = gringotts.exercise(optionId, creds.address).encodeFunctionCall()
    val tx = createTx(creds.address, encodedFunction)
    return executeTransaction(tx)
  }

  private fun createTx(sender: String, encodedFunction: String, amount: BigInteger? = null): Transaction = Transaction.createFunctionCallTransaction(
      sender,
      web3j.getLatestNonce(sender),
      DefaultGasProvider.GAS_PRICE,
      DefaultGasProvider.GAS_LIMIT,
      gringotts.contractAddress,
      amount,
      encodedFunction
  )

  private fun executeTransaction(tx: Transaction): TransactionReceipt {
    val txResponse = web3j.ethSendTransaction(tx).send()
    val txHash = txResponse.transactionHash

    do {
      Thread.sleep(100)
    } while (web3j.ethGetTransactionReceipt(txHash).send().transactionReceipt.isEmpty)

    return web3j.ethGetTransactionReceipt(txHash).send().transactionReceipt.get()
  }

  private fun Web3j.getLatestNonce(address: String): BigInteger = this.ethGetTransactionCount(address, DefaultBlockParameterName.LATEST).send().transactionCount
}