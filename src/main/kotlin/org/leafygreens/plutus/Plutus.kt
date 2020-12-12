package org.leafygreens.plutus;

import ch.qos.logback.classic.Level
import org.leafygreens.plutus.generated.contracts.HelloWorld
import io.epirus.web3j.Epirus
import io.epirus.web3j.gas.EpirusGasProvider
import io.epirus.web3j.gas.GasPrice
import org.slf4j.LoggerFactory
import org.web3j.crypto.Credentials
import org.web3j.crypto.WalletUtils
import org.web3j.protocol.Network
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import org.web3j.tx.gas.ContractGasProvider
import java.nio.file.Paths
import kotlin.system.exitProcess

/**
 * <p>Auto generated code.
 * <p><strong>Do not modify!</strong>
 * <p>This is the generated class for <code>epirus new helloworld</code></p>
 * <p>It deploys the Hello World contract in src/main/solidity/ and prints its address</p>
 * <p>The generated tests, if generated, are in src/test/java</p>
 * <p>For more information on how to run this project, please refer to our <a href="https://docs.epirus.io/quickstart/#deployment">documentation</a></p>
 */
class Plutus {

    private val log = LoggerFactory.getILoggerFactory().getLogger("org.web3j.protocol.http.HttpService")
    private val epirusDeploy = System.getenv().getOrDefault("EPIRUS_DEPLOY", "false")!!.toBoolean()
    private val deployNetwork = Network.valueOf(System.getenv().getOrDefault("WEB3J_NETWORK", "rinkeby").toUpperCase())
    private val walletPath = System.getenv("WEB3J_WALLET_PATH")
    private val walletPassword = System.getenv().getOrDefault("WEB3J_WALLET_PASSWORD", "")
    private val nodeUrl = System.getenv().getOrDefault("WEB3J_NODE_URL", System.getProperty("WEB3J_NODE_URL"))


    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Plutus().start(args)
        }
    }

    fun start(args: Array<String>) {
        log as ch.qos.logback.classic.Logger
        log.level = if (epirusDeploy) Level.ERROR else Level.INFO

        if (!epirusDeploy && (walletPath == null || walletPassword.isBlank() || nodeUrl == null)) {
            log.info("As the application isn't being run using the Epirus Platform, the following environment variables are expected: " +
                    "WEB3J_WALLET_PATH, WEB3J_WALLET_PASSWORD, WEB3J_NODE_URL. Please ensure these are set and try again.");
            exitProcess(-1);
        }
        try {
            val credentials: Credentials? = WalletUtils.loadCredentials(walletPassword, Paths.get(walletPath).toFile())
            val web3j: Web3j? = getDeployWeb3j()
            val helloWorld: HelloWorld? = deployHelloWorld(web3j, credentials, EpirusGasProvider(deployNetwork, GasPrice.High))
            callGreetMethod(helloWorld)
        } catch (e: Exception) {
            log.error(e.message);
        }
    }

    private fun getDeployWeb3j(): Web3j {
        return if (nodeUrl == null || nodeUrl.isEmpty()) {
            Epirus.buildWeb3j(deployNetwork) // deployNetwork = rinkeby/ropsten/mainnet
        } else {
            log.info("Connecting to $nodeUrl")
            Web3j.build(HttpService(nodeUrl))
        }
    }

    @Throws(Exception::class)
    private fun deployHelloWorld(
            web3j: Web3j?,
            credentials: Credentials?,
            contractGasProvider: ContractGasProvider?
    ): HelloWorld? {
        return HelloWorld.deploy(web3j, credentials, contractGasProvider, "Hello Blockchain World!").send()
    }

    @Throws(Exception::class)
    private fun callGreetMethod(helloWorld: HelloWorld?) {
        log.info("Calling the greeting method of contract HelloWorld")
        val response: String = helloWorld?.greeting()!!.send()
        log.info("Contract returned: $response")
        val contractAddress = if(epirusDeploy) {
            "https://${deployNetwork.networkName}.epirus.io/contracts/${helloWorld.contractAddress}"
        } else {
            helloWorld.contractAddress
        }
        println(String.format("%-20s", "Contract address") + contractAddress)
        exitProcess(0)
    }
}
