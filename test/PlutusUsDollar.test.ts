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

describe("Plutus Us Dollar", () => {
  let deployer: Signer;
  let others: Signer[];

  let pUsdFactory: PlutusUsDollarFactory;
  let pUsd: PlutusUsDollar;

  beforeEach(async () => {
    [deployer, ...others] = await ethers.getSigners();
    pUsdFactory = new PlutusUsDollarFactory(deployer);
    pUsd = await pUsdFactory.deploy();
  });

  it("starts with 0 balance", async () => {
    const deployerAddress = await deployer.getAddress();
    const randoAddress = await others[0].getAddress();
    expect(await pUsd.totalSupply()).to.equal(0);
    expect(await pUsd.balanceOf(deployerAddress)).to.equal(0);
    expect(await pUsd.balanceOf(randoAddress)).to.equal(0);
  });

  it("allows minting to non owners", async () => {
    const amount = 1000
    const randoAddress = await others[0].getAddress();
    await pUsd.mint(randoAddress, amount);
    expect(await pUsd.totalSupply()).to.equal(amount);
    expect(await pUsd.balanceOf(randoAddress)).to.equal(amount);
  });

  // todo how to *confirm* that external contracts cant call mint

  it("allows tokens to be burned", async () => {
    const amount = 1000
    const randoAddress = await others[0].getAddress();
    await pUsd.mint(randoAddress, amount);
    await pUsd.burn(randoAddress, 250);
    expect(await pUsd.totalSupply()).to.equal(750);
    expect(await pUsd.balanceOf(randoAddress)).to.equal(750);
  });

  // todo add tests for event emitters

})