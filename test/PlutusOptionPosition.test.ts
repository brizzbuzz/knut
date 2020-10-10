import {ethers} from "@nomiclabs/buidler";
import {Signer} from "ethers";
import chai, {expect} from "chai";
import {solidity} from "ethereum-waffle";
import {PlutusOptionPositionFactory} from "../typechain";
import {PlutusOptionPosition} from "../typechain/PlutusOptionPosition";

chai.use(solidity);

describe("Plutus Option Position", () => {
  let deployer: Signer;
  let others: Signer[];

  let popFactory: PlutusOptionPositionFactory;
  let pop: PlutusOptionPosition;

  beforeEach(async () => {
    [deployer, ...others] = await ethers.getSigners();
    popFactory = new PlutusOptionPositionFactory(deployer);
    pop = await popFactory.deploy();
  });

  it("minting saves position metadata", async () => {
    const randomAddress = await others[0].getAddress();
    await pop.mint(randomAddress, 1, 500);
    expect(await pop.totalSupply()).to.equal(1);
    expect(await pop.balanceOf(randomAddress)).to.equal(1);
    expect(await pop.ownerOf(1)).to.equal(randomAddress);
    expect((await pop.checkPosition(1))[0]).to.equal(1);
    expect((await pop.checkPosition(1))[1]).to.equal(500);
  });

  it("Allows token to be burned", async () => {
    const randomAddress = await others[0].getAddress();
    await pop.mint(randomAddress, 1, 500);
    expect(await pop.ownerOf(1)).to.equal(randomAddress);
    await pop.burn(randomAddress, 1);
    expect(await pop.balanceOf(randomAddress)).to.equal(0);
  });

  it("Prevents exercising tokens you don't hold", async () => {
    const ownerAddress = await others[0].getAddress();
    const phonyAddress = await others[1].getAddress();
    await pop.mint(ownerAddress, 1, 500);
    await expect(pop.burn(phonyAddress, 1))
      .to.be.revertedWith("You must own this option in order to exercise");
  })
})