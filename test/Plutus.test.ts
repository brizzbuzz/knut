import {ethers} from "@nomiclabs/buidler";
import {Plutus} from "../typechain/Plutus";
import {Signer} from "ethers";
import {expect, use} from "chai";
import {deployMockContract, MockContract, solidity} from "ethereum-waffle";
import {PlutusFactory, PlutusOptionPositionFactory, PlutusUsDollarFactory, PlutusVaultFactory} from "../typechain";
import {PlutusUsDollar} from "../typechain/PlutusUsDollar";
import {PlutusOptionPosition} from "../typechain/PlutusOptionPosition";
import {PlutusVault} from "../typechain/PlutusVault";
import {waffleChai} from "@nomiclabs/buidler-waffle/dist/waffle-chai";

const OracleDefinition = require("../artifacts/UniswapAnchoredView.json");
const {ether} = require('@openzeppelin/test-helpers');

use(solidity);
use(waffleChai);

describe("Plutus", () => {
  // Addresses
  let deployer: Signer;
  let others: Signer[];

  // Plutus Factories
  let plutusFactory: PlutusFactory;
  let pUsdFactory: PlutusUsDollarFactory;
  let popFactory: PlutusOptionPositionFactory;
  let vaultFactory: PlutusVaultFactory;

  // Plutus Contracts
  let plutus: Plutus;
  let pUsd: PlutusUsDollar;
  let pop: PlutusOptionPosition;
  let vault: PlutusVault;

  // External Contracts
  let mockOracle: MockContract;

  beforeEach(async () => {
    [deployer, ...others] = await ethers.getSigners();

    mockOracle = await deployMockContract(deployer, OracleDefinition.abi)

    plutusFactory = new PlutusFactory(deployer);
    pUsdFactory = new PlutusUsDollarFactory(deployer);
    popFactory = new PlutusOptionPositionFactory(deployer);
    vaultFactory = new PlutusVaultFactory(deployer);

    plutus = await plutusFactory.deploy(mockOracle.address);
    vault = await vaultFactory.attach(await plutus.Vault());
    pop = await popFactory.attach(await plutus.POP());
    pUsd = await pUsdFactory.attach(await plutus.pUSD());

    // TODO Need to ensure this is overridable in individual tests
    await mockOracle.mock.price.returns(500);
  });

  it("sets pointers", async () => {
    expect(await plutus.POP()).to.equal(pop.address);
    expect(await plutus.pUSD()).to.equal(pUsd.address);
    expect(await plutus.Vault()).to.equal(vault.address);
    expect(await plutus.Oracle()).to.equal(mockOracle.address);
  });

  it("Plutus can lock up user funds into Vault and returns pUSD and POP", async () => {
    const amount = ether('42');
    const payee = await others[0].getAddress();
    await plutus.lockup(payee, {value: amount});
    expect(await vault.depositsOf(payee)).to.equal(amount);
    expect(await pUsd.balanceOf(payee)).to.equal(500);
    expect(await pop.ownerOf(1)).to.equal(payee);
  });

  it("Randos cannot deposit funds into the Vault", async () => {
    const amount = ether('42');
    const rando = await others[0].getAddress();
    await expect(vault.deposit(rando, {value: amount}))
      .to.be.revertedWith("Ownable: caller is not the owner");
  });

  it("Funds cannot be directly withdrawn from Vault", async () => {
    const amount = ether('42');
    const payee = await others[0].getAddress();
    await plutus.lockup(payee, { value: amount });
    await expect(vault.withdraw(payee, payee, amount))
      .to.be.revertedWith("Ownable: caller is not the owner");
  });

  it("Randos cannot mint pUSD", async () => {
    const rando = await others[0].getAddress();
    await expect(pUsd.mint(rando, 10000000))
      .to.be.revertedWith("Ownable: caller is not the owner");
  });

  it("Randos cannot burn pUSD", async () => {
    const amount = ether('42');
    const payee = await others[0].getAddress();
    await plutus.lockup(payee, {value: amount});
    await expect(pUsd.burn(payee, 500))
      .to.be.revertedWith("Ownable: caller is not the owner");
  });

  it("Allows Option Holder to exercise", async () => {
    const amount = ether('42');
    const payee = await others[0].getAddress();
    await plutus.lockup(payee, {value: amount});
    expect(await vault.depositsOf(payee)).to.equal(amount);
    expect(await pUsd.balanceOf(payee)).to.equal(500);
    expect(await pop.ownerOf(1)).to.equal(payee);
    await expect(await plutus.exercise(1, payee)).to.changeBalance(await others[0], amount);
    expect(await vault.depositsOf(payee)).to.equal(0);
    expect(await pop.balanceOf(payee)).to.equal(0);
    expect(await pUsd.balanceOf(payee)).to.equal(0);
  });

  // TODO Edge Cases Galore!!
})