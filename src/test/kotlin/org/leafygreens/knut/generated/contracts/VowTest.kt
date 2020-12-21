package org.leafygreens.knut.generated.contracts

import java.math.BigDecimal
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.leafygreens.knut.generated.contracts.TestUtils.generateFundedCreds
import org.web3j.EVMTest
import org.web3j.protocol.Web3j
import org.web3j.tx.TransactionManager
import org.web3j.tx.gas.ContractGasProvider
import org.web3j.utils.Convert

@EVMTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VowTest {

  private lateinit var web3j: Web3j
  private lateinit var transactionManager: TransactionManager
  private lateinit var contractGasProvider: ContractGasProvider
  private lateinit var vows: UnbreakableVow

  @BeforeAll
  fun setup(
      web3j: Web3j,
      transactionManager: TransactionManager,
      contractGasProvider: ContractGasProvider
  ) {
    this.web3j = web3j
    this.transactionManager = transactionManager
    this.contractGasProvider = contractGasProvider

    vows = UnbreakableVow.deploy(web3j, transactionManager, contractGasProvider).send()
  }

  @Test
  fun `Vows are tradable`() {
    // when
    val creds = web3j.generateFundedCreds(BigDecimal.ONE)
    val lockupAmount = Convert.toWei(BigDecimal(0.5), Convert.Unit.ETHER)

  }

  @Test
  fun `Only holder can transfer Vows`() {

  }

  @Test
  fun `Only contract owner can mint or burn Vows`() {

  }
}