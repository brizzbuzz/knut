import {ethers} from "@nomiclabs/buidler";
import {Signer} from "ethers";
import chai, {expect} from "chai";
import {solidity} from "ethereum-waffle";
import {PlutusVault} from "../typechain/PlutusVault";
import {PlutusVaultFactory} from "../typechain";

const {ether} = require('@openzeppelin/test-helpers');

chai.use(solidity);

describe("Plutus Vault", () => {
  let deployer: Signer;
  let others: Signer[];

  let vaultFactory: PlutusVaultFactory;
  let vault: PlutusVault;

  beforeEach(async () => {
    [deployer, ...others] = await ethers.getSigners();
    vaultFactory = new PlutusVaultFactory(deployer);
    vault = await vaultFactory.deploy();
  });

  it("Allows user deposits", async () => {
    const amount = ether('2');
    const payee = await others[0].getAddress();
    await vault.deposit(payee, { value: amount });
    expect(await vault.depositsOf(payee)).to.equal(amount);
  });

  it("Allows non depositor withdrawals", async () => {
    const amount = ether('2');
    const first = await others[0].getAddress();
    const second = await others[1].getAddress();
    await vault.deposit(first, { value: amount });
    await expect(await vault.withdraw(second, first, amount)).to.changeBalance(await others[1], amount);
    expect(await vault.depositsOf(first)).to.equal(0);
  });

});