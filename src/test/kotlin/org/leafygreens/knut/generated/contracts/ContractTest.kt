package org.leafygreens.knut.generated.contracts

import org.web3j.protocol.Web3j
import org.web3j.tx.TransactionManager
import org.web3j.tx.gas.ContractGasProvider

abstract class ContractTest {

  lateinit var web3j: Web3j
  lateinit var transactionManager: TransactionManager
  lateinit var contractGasProvider: ContractGasProvider

}