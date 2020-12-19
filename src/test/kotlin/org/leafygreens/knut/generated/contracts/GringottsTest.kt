package org.leafygreens.knut.generated.contracts

import java.math.BigDecimal
import java.math.BigInteger
import org.junit.jupiter.api.Assertions.assertEquals
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
  private lateinit var vault713: Vault713

  @BeforeAll
  fun setup(
      web3j: Web3j,
      transactionManager: TransactionManager,
      contractGasProvider: ContractGasProvider
  ) {
    gringotts = Gringotts.deploy(web3j, transactionManager, contractGasProvider).send()
    knut = Knut.load(gringotts.knut().send(), web3j, transactionManager, contractGasProvider)
    vows = UnbreakableVow.load(gringotts.vows().send(), web3j, transactionManager, contractGasProvider)
    vault713 = Vault713.load(gringotts.vault().send(), web3j, transactionManager, contractGasProvider)

    this.web3j = web3j
    this.transactionManager = transactionManager
    this.contractGasProvider = contractGasProvider
  }

  @Test
  fun `Can lockup funds in a vault`() {
    // when
    val creds = generateFundedCreds(BigDecimal.ONE, web3j)
    val startingBalance = web3j.ethGetBalance(creds.address, DefaultBlockParameterName.LATEST).send().balance
    val lockupAmount = Convert.toWei(BigDecimal(0.5), Convert.Unit.ETHER)

    // do
    val lockupReceipt = performSimpleLockup(creds, lockupAmount)
    val lockupEvents = gringotts.getLockupEvents(lockupReceipt)
    val depositEvents = vault713.getDepositedEvents(lockupReceipt)

    // expect
    val gasCost = lockupReceipt.gasUsed.times(contractGasProvider.gasPrice)
    val newBalance = startingBalance
        .minus(lockupAmount.toBigInteger())
        .minus(gasCost)

    assertEquals(1, lockupEvents.size, "There should only be one lockup event")
    assertEquals(1, depositEvents.size, "There should only be one deposit event")
    assertEquals(
        newBalance,
        web3j.ethGetBalance(creds.address, DefaultBlockParameterName.LATEST).send().balance,
        "Account balance should be ($startingBalance - $gasCost - $lockupAmount)"
    )
    assertEquals(
        lockupAmount.toBigInteger(),
        vault713.depositsOf(creds.address).send(),
        "$lockupAmount should be locked up by ${creds.address}"
    )
  }

  @Test
  fun `Can withdraw funds from a vault`() {
    // when
    val creds = generateFundedCreds(BigDecimal.ONE, web3j)
    val startingBalance = web3j.ethGetBalance(creds.address, DefaultBlockParameterName.LATEST).send().balance
    val lockupAmount = Convert.toWei(BigDecimal(0.5), Convert.Unit.ETHER)
    val lockupReceipt = performSimpleLockup(creds, lockupAmount)
    val lockupEvent = gringotts.getLockupEvents(lockupReceipt).first()

    // do
    val exerciseReceipt = performSimpleExercise(creds, lockupEvent.optionID)
    val exerciseEvents = gringotts.getExerciseEvents(exerciseReceipt)
    val withdrawEvents = vault713.getWithdrawnEvents(exerciseReceipt)

    // expect
    val lockupGasCost = lockupReceipt.gasUsed.times(contractGasProvider.gasPrice)
    val exerciseGasCost = exerciseReceipt.gasUsed.times(contractGasProvider.gasPrice)
    val newBalance = startingBalance
        .minus(lockupGasCost)
        .minus(exerciseGasCost)

    assertEquals(1, withdrawEvents.size, "There should only be one withdraw event")
    assertEquals(1, exerciseEvents.size, "There should only be one exercise event")
    assertEquals(BigInteger.ZERO, vault713.depositsOf(creds.address).send())
    assertEquals(
        newBalance,
        web3j.ethGetBalance(creds.address, DefaultBlockParameterName.LATEST).send().balance,
        "Account balance should be (starting - lockup gas - exercise gas)"
    )
  }

  @Test
  fun `Can mint knuts on lockup`() {
    // when
    val creds = generateFundedCreds(BigDecimal.ONE, web3j)
    val startingBalance = web3j.ethGetBalance(creds.address, DefaultBlockParameterName.LATEST).send().balance
    val lockupAmount = Convert.toWei(BigDecimal(0.5), Convert.Unit.ETHER)

    // do
    val lockupReceipt = performSimpleLockup(creds, lockupAmount)
    val lockupEvents = gringotts.getLockupEvents(lockupReceipt)
    val mintEvents = knut.getMintEvents(lockupReceipt)

    // expect
    assertEquals(1, lockupEvents.size, "There should only be one lockup event")
    assertEquals(1, mintEvents.size, "There should only be one mint event")
    assertEquals(
        BigInteger.valueOf(500),
        knut.balanceOf(creds.address).send(),
        "500 knut should be minted and given to ${creds.address}"
    )
  }

  @Test
  fun `Can burn knuts on exercise`() {
    // when
    val creds = generateFundedCreds(BigDecimal.ONE, web3j)
    val lockupAmount = Convert.toWei(BigDecimal(0.5), Convert.Unit.ETHER)
    val lockupReceipt = performSimpleLockup(creds, lockupAmount)
    val lockupEvent = gringotts.getLockupEvents(lockupReceipt).first()

    // do
    val exerciseReceipt = performSimpleExercise(creds, lockupEvent.optionID)
    val exerciseEvents = gringotts.getExerciseEvents(exerciseReceipt)
    val burnEvents = knut.getBurnEvents(exerciseReceipt)

    // expect
    assertEquals(1, burnEvents.size, "There should only be one burn event")
    assertEquals(1, exerciseEvents.size, "There should only be one exercise event")
    assertEquals(
        BigInteger.ZERO,
        knut.balanceOf(creds.address).send(),
        "All knut should be burned from ${creds.address}"
    )
  }

  @Test
  fun `Can create a vow on lockup`() {
    // when
    val creds = generateFundedCreds(BigDecimal.ONE, web3j)
    val startingBalance = web3j.ethGetBalance(creds.address, DefaultBlockParameterName.LATEST).send().balance
    val lockupAmount = Convert.toWei(BigDecimal(0.5), Convert.Unit.ETHER)

    // do
    val lockupReceipt = performSimpleLockup(creds, lockupAmount)
    val lockupEvents = gringotts.getLockupEvents(lockupReceipt)
    val mintEvents = vows.getMintEvents(lockupReceipt)

    // expect
    val optionId = mintEvents.first().optionId

    assertEquals(1, lockupEvents.size, "There should only be one lockup event")
    assertEquals(1, mintEvents.size, "There should only be one deposit event")

    assertEquals(lockupAmount.toBigInteger(), vows.checkPositionValue(optionId).send())
    assertEquals(BigInteger.valueOf(500), vows.checkPositionCost(optionId).send())
    assertEquals(creds.address, vows.ownerOf(optionId).send())
  }

  @Test
  fun `Can burn a vow on exercise`() {
    // when
    val creds = generateFundedCreds(BigDecimal.ONE, web3j)
    val lockupAmount = Convert.toWei(BigDecimal(0.5), Convert.Unit.ETHER)
    val lockupReceipt = performSimpleLockup(creds, lockupAmount)
    val lockupEvent = gringotts.getLockupEvents(lockupReceipt).first()

    // do
    val exerciseReceipt = performSimpleExercise(creds, lockupEvent.optionID)
    val exerciseEvents = gringotts.getExerciseEvents(exerciseReceipt)
    val burnEvents = vows.getBurnEvents(exerciseReceipt)

    // expect
    assertEquals(1, exerciseEvents.size, "There should only be one lockup event")
    assertEquals(1, burnEvents.size, "There should only be one deposit event")
  }

  private fun performSimpleLockup(creds: Credentials, lockupAmount: BigDecimal): TransactionReceipt {
    val lockup = gringotts.lockup().encodeFunctionCall()
    val tx = createTx(creds.address, lockup, lockupAmount.toBigInteger())
    return executeTransaction(tx)
  }

  private fun performSimpleExercise(creds: Credentials, optionId: BigInteger): TransactionReceipt {
    val encodedFunction = gringotts.exercise(optionId).encodeFunctionCall()
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