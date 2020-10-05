import {ethers} from "@nomiclabs/buidler";
import {Signer} from "ethers";
import chai, {expect} from "chai";
import {solidity} from "ethereum-waffle";
import {PlutusUsDollarFactory} from "../typechain";
import {PlutusUsDollar} from "../typechain/PlutusUsDollar";

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

  it("can be minted", async () => {
    const amount = 1000
    const randoAddress = await others[0].getAddress();
    await pUsd.mint(randoAddress, amount);
    expect(await pUsd.totalSupply()).to.equal(amount);
    expect(await pUsd.balanceOf(randoAddress)).to.equal(amount);
  });

  it("can be burned", async () => {
    const amount = 1000
    const randoAddress = await others[0].getAddress();
    await pUsd.mint(randoAddress, amount);
    await pUsd.burn(randoAddress, 250);
    expect(await pUsd.totalSupply()).to.equal(750);
    expect(await pUsd.balanceOf(randoAddress)).to.equal(750);
  });

  it("does not allow burning of extra tokens", async () => {
    const randoAddress = await others[0].getAddress();
    await expect(pUsd.burn(randoAddress, 100))
      .to.be.revertedWith("ERC20: burn amount exceeds balance");
  });

  // todo add tests for event emitters

})