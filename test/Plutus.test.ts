import {ethers} from "@nomiclabs/buidler";
import {Plutus} from "../typechain/Plutus";
import {Signer} from "ethers";
import chai from "chai";
import {solidity} from "ethereum-waffle";
import {PlutusFactory, PlutusOptionPositionFactory, PlutusUsDollarFactory, PlutusVaultFactory} from "../typechain";
import {PlutusUsDollar} from "../typechain/PlutusUsDollar";
import {PlutusOptionPosition} from "../typechain/PlutusOptionPosition";
import {expect} from "chai";
import {PlutusVault} from "../typechain/PlutusVault";
const { balance, ether, expectEvent, expectRevert } = require('@openzeppelin/test-helpers');


chai.use(solidity)

describe("Plutus", () => {
  let deployer: Signer;

  let plutusFactory: PlutusFactory;
  let pUsdFactory: PlutusUsDollarFactory;
  let popFactory: PlutusOptionPositionFactory;
  let vaultFactory: PlutusVaultFactory;

  let plutus: Plutus;
  let pUsd: PlutusUsDollar;
  let pop: PlutusOptionPosition;
  let vault: PlutusVault;

  beforeEach(async () => {
    [deployer] = await ethers.getSigners();

    plutusFactory = new PlutusFactory(deployer);
    pUsdFactory = new PlutusUsDollarFactory(deployer);
    popFactory = new PlutusOptionPositionFactory(deployer);
    vaultFactory = new PlutusVaultFactory(deployer);

    pUsd = await pUsdFactory.deploy();
    pop = await popFactory.deploy();
    vault = await vaultFactory.deploy();
    plutus = await plutusFactory.deploy(pUsd.address, pop.address, vault.address);
  });

  it("sets pointers", async () => {
    expect(await plutus.POP()).to.equal(pop.address);
    expect(await plutus.pUSD()).to.equal(pUsd.address);
    expect(await plutus.Vault()).to.equal(vault.address);
  });

  it("only allows Plutus to control the Vault", async () => {
    const amount = ether('42');
    await vault.deposit(plutus.address, { value: amount });
    expect(await vault.depositsOf(plutus.address)).to.equal(amount);
    expectRevert(vault.deposit(pop.address, { value: amount }));
  });

})