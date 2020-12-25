package org.leafygreens.knut.generated.contracts

import java.math.BigDecimal
import java.math.BigInteger
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.leafygreens.knut.generated.contracts.TestUtils.createTx
import org.leafygreens.knut.generated.contracts.TestUtils.executeTransaction
import org.leafygreens.knut.generated.contracts.TestUtils.generateFundedCreds
import org.web3j.EVMTest
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.response.TransactionReceipt
import org.web3j.tx.TransactionManager
import org.web3j.tx.exceptions.ContractCallException
import org.web3j.tx.gas.ContractGasProvider
import org.web3j.utils.Convert

@EVMTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GringottsTest : ContractTest() {

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
    this.web3j = web3j
    this.transactionManager = transactionManager
    this.contractGasProvider = contractGasProvider

    gringotts = Gringotts.deploy(web3j, transactionManager, contractGasProvider).send()
    knut = Knut.load(gringotts.knut().send(), web3j, transactionManager, contractGasProvider)
    vows = UnbreakableVow.load(gringotts.vows().send(), web3j, transactionManager, contractGasProvider)
    vault713 = Vault713.load(gringotts.vault().send(), web3j, transactionManager, contractGasProvider)
  }

  @Test
  fun `Can lockup funds in a vault`() {
    // when
    val creds = web3j.generateFundedCreds()
    val startingBalance = creds.getCurrentBalance()
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
    assertEquals(newBalance, creds.getCurrentBalance(), "Account balance should be ($startingBalance - $gasCost - $lockupAmount)")
    assertEquals(lockupAmount.toBigInteger(), vault713.depositsOf(creds.address).send(), "$lockupAmount should be locked up by ${creds.address}")
  }

  @Test
  fun `Can withdraw funds from a vault`() {
    // when
    val creds = web3j.generateFundedCreds()
    val startingBalance = creds.getCurrentBalance()
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
    assertEquals(newBalance, creds.getCurrentBalance(), "Account balance should be (starting - lockup gas - exercise gas)")
  }

  @Test
  fun `Can mint knuts on lockup`() {
    // when
    val creds = web3j.generateFundedCreds()
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
    val creds = web3j.generateFundedCreds()
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
    val creds = web3j.generateFundedCreds()
    val lockupAmount = Convert.toWei(BigDecimal(0.5), Convert.Unit.ETHER)

    // do
    val lockupReceipt = performSimpleLockup(creds, lockupAmount)
    val lockupEvents = gringotts.getLockupEvents(lockupReceipt)
    val mintEvents = vows.getSwearEvents(lockupReceipt)

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
    val creds = web3j.generateFundedCreds()
    val lockupAmount = Convert.toWei(BigDecimal(0.5), Convert.Unit.ETHER)
    val lockupReceipt = performSimpleLockup(creds, lockupAmount)
    val lockupEvent = gringotts.getLockupEvents(lockupReceipt).first()

    // do
    val exerciseReceipt = performSimpleExercise(creds, lockupEvent.optionID)
    val exerciseEvents = gringotts.getExerciseEvents(exerciseReceipt)
    val burnEvents = vows.getFulfillEvents(exerciseReceipt)

    // expect
    val optionId = exerciseEvents.first().optionId
    assertEquals(1, exerciseEvents.size, "There should only be one lockup event")
    assertEquals(1, burnEvents.size, "There should only be one deposit event")
    assertEquals(optionId, burnEvents.first().optionId, "Exercise option id should match burned id")
    assertThrows<ContractCallException>("Burned token should have no owner") {
      vows.ownerOf(optionId).send()
    }
  }

  @Test
  fun `Can create multiple lockups`() {
    // when
    val creds = web3j.generateFundedCreds()
    val startingBalance = creds.getCurrentBalance()
    val lockupAmountA = Convert.toWei(BigDecimal(0.2), Convert.Unit.ETHER)
    val lockupAmountB = Convert.toWei(BigDecimal(0.05), Convert.Unit.ETHER)

    // do
    val lockupReceiptA = performSimpleLockup(creds, lockupAmountA)
    val lockupReceiptB = performSimpleLockup(creds, lockupAmountB)
    val lockupEventA = gringotts.getLockupEvents(lockupReceiptA).first()
    val lockupEventB = gringotts.getLockupEvents(lockupReceiptB).first()

    // expect
    val optionIdA = lockupEventA.optionID
    val optionIdB = lockupEventB.optionID

    assertEquals(creds.address, vows.ownerOf(optionIdA).send())
    assertEquals(creds.address, vows.ownerOf(optionIdB).send())
    assertEquals(lockupAmountA.plus(lockupAmountB).toBigInteger(), vault713.depositsOf(creds.address).send())

    val newBalance = startingBalance
        .minus(lockupReceiptA.gasUsed.times(contractGasProvider.gasPrice))
        .minus(lockupReceiptB.gasUsed.times(contractGasProvider.gasPrice))
        .minus(lockupAmountA.toBigInteger())
        .minus(lockupAmountB.toBigInteger())

    assertEquals(newBalance, creds.getCurrentBalance())
  }

  @Test
  fun `Traded vows can be exercised`() {
    // when
    val creator = web3j.generateFundedCreds()
    val buyer = web3j.generateFundedCreds()
    val startingBalance = creator.getCurrentBalance()
    val lockupAmount = Convert.toWei(BigDecimal(0.2), Convert.Unit.ETHER)
    val lockupReceipt = performSimpleLockup(creator, lockupAmount)
    val lockupEvent = gringotts.getLockupEvents(lockupReceipt).first()

    // do
    performKnutTransfer(creator, buyer, lockupEvent.value)
    performVowTransfer(creator, buyer, lockupEvent.optionID)
    val exerciseReceipt = performSimpleExercise(buyer, lockupEvent.optionID)
    val exerciseEvent = gringotts.getExerciseEvents(exerciseReceipt).first()

    // expect
    assertEquals(BigInteger.ZERO, knut.balanceOf(creator.address).send())
    assertTrue(buyer.getCurrentBalance() > creator.getCurrentBalance())
  }

  @Test
  fun `Traded vows cannot be exercised by creator`() {

  }

  @Test
  fun `Vows cannot be exercised by non-holders`() {

  }

  @Test
  fun `Cannot exercise without required Knuts`() {

  }

  @Test
  fun `Double exercise cannot be performed`() {

  }

  @Test
  fun `User cannot lock up more ETH than they have`() {

  }

  private fun performSimpleLockup(creds: Credentials, lockupAmount: BigDecimal): TransactionReceipt {
    val lockup = gringotts.lockup().encodeFunctionCall()
    val tx = web3j.createTx(creds.address, gringotts.contractAddress, lockup, lockupAmount)
    return web3j.executeTransaction(tx)
  }

  private fun performSimpleExercise(creds: Credentials, optionId: BigInteger): TransactionReceipt {
    val encodedFunction = gringotts.exercise(optionId).encodeFunctionCall()
    val tx = web3j.createTx(creds.address, gringotts.contractAddress, encodedFunction)
    return web3j.executeTransaction(tx)
  }

  // TODO Probably pop out to test utils and swap creds extension for Web3j
  private fun Credentials.getCurrentBalance() = web3j.ethGetBalance(this.address, DefaultBlockParameterName.LATEST).send().balance

  private fun performKnutTransfer(from: Credentials, to: Credentials, amount: BigInteger): TransactionReceipt {
    val transferFunction = knut.transferFrom(from.address, to.address, amount).encodeFunctionCall()
    val tx = web3j.createTx(from.address, knut.contractAddress, transferFunction)
    return web3j.executeTransaction(tx)
  }

  private fun performVowTransfer(from: Credentials, to: Credentials, optionId: BigInteger): TransactionReceipt {
    val transferFunction = vows.transferFrom(from.address, to.address, optionId).encodeFunctionCall()
    val tx = web3j.createTx(from.address, vows.contractAddress, transferFunction)
    return web3j.executeTransaction(tx)
  }

  private fun attemptTheft(from: Credentials, to: Credentials, amount: BigInteger): TransactionReceipt {
    val transferFunction = knut.transferFrom(from.address, to.address, amount).encodeFunctionCall()
    val tx = web3j.createTx(to.address, knut.contractAddress, transferFunction)
    return web3j.executeTransaction(tx)
  }
}