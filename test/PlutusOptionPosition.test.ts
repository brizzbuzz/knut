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

})