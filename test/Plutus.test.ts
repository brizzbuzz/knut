import {ethers} from "@nomiclabs/buidler";
import {Plutus} from "../typechain/Plutus";
import {Signer} from "ethers";
import chai, {expect} from "chai";
import {solidity} from "ethereum-waffle";
import {PlutusFactory, PlutusOptionPositionFactory, PlutusUsDollarFactory, PlutusVaultFactory} from "../typechain";
import {PlutusUsDollar} from "../typechain/PlutusUsDollar";
import {PlutusOptionPosition} from "../typechain/PlutusOptionPosition";
import {PlutusVault} from "../typechain/PlutusVault";

const {ether} = require('@openzeppelin/test-helpers');

chai.use(solidity)

describe("Plutus", () => {
  let deployer: Signer;
  let others: Signer[];

  let plutusFactory: PlutusFactory;
  let pUsdFactory: PlutusUsDollarFactory;
  let popFactory: PlutusOptionPositionFactory;
  let vaultFactory: PlutusVaultFactory;

  let plutus: Plutus;
  let pUsd: PlutusUsDollar;
  let pop: PlutusOptionPosition;
  let vault: PlutusVault;

  beforeEach(async () => {
    [deployer, ...others] = await ethers.getSigners();

    plutusFactory = new PlutusFactory(deployer);
    pUsdFactory = new PlutusUsDollarFactory(deployer);
    popFactory = new PlutusOptionPositionFactory(deployer);
    vaultFactory = new PlutusVaultFactory(deployer);

    plutus = await plutusFactory.deploy();
    vault = await vaultFactory.attach(await plutus.Vault());
    pop = await popFactory.attach(await plutus.POP());
    pUsd = await pUsdFactory.attach(await plutus.pUSD());
  });

  it("sets pointers", async () => {
    expect(await plutus.POP()).to.equal(pop.address);
    expect(await plutus.pUSD()).to.equal(pUsd.address);
    expect(await plutus.Vault()).to.equal(vault.address);
  });

  it("Plutus can lock up user funds into Vault and return pUSD", async () => {
    const amount = ether('42');
    const payee = await others[0].getAddress();
    await plutus.lockup(payee, {value: amount});
    expect(await vault.depositsOf(payee)).to.equal(amount);
    expect(await pUsd.balanceOf(payee)).to.equal(500);
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
    await expect(vault.withdraw(payee))
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
  })

})