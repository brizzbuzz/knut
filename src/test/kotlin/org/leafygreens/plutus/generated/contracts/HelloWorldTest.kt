package org.leafygreens.plutus.generated.contracts

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.web3j.EVMTest
import org.web3j.protocol.Web3j
import org.web3j.tx.TransactionManager
import org.web3j.tx.gas.ContractGasProvider

@EVMTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HelloWorldTest {
  private lateinit var helloWorld: HelloWorld

  @Test
  fun greeting() {
    val stringVar = helloWorld.greeting().send()
    Assertions.assertEquals("REPLACE_ME", stringVar)
  }

  @Test
  fun newGreeting() {
    val transactionReceiptVar = helloWorld.newGreeting("REPLACE_ME").send()
    Assertions.assertTrue(transactionReceiptVar.isStatusOK())
  }

  @BeforeAll
  fun deploy(
    web3j: Web3j,
    transactionManager: TransactionManager,
    contractGasProvider: ContractGasProvider
  ) {
     helloWorld = HelloWorld.deploy(web3j, transactionManager, contractGasProvider,
        "REPLACE_ME").send()
  }
}
