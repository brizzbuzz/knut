import {ethers} from "@nomiclabs/buidler";
import {Plutus} from "../typechain/Plutus";
import {Signer} from "ethers";
import chai from "chai";
import {solidity} from "ethereum-waffle";
import {PlutusFactory, PlutusOptionPositionFactory, PlutusUsDollarFactory} from "../typechain";
import {PlutusUsDollar} from "../typechain/PlutusUsDollar";
import {PlutusOptionPosition} from "../typechain/PlutusOptionPosition";
import {expect} from "chai";

chai.use(solidity)

describe("Plutus", () => {
  let deployer: Signer;

  let plutusFactory: PlutusFactory;
  let pUsdFactory: PlutusUsDollarFactory;
  let popFactory: PlutusOptionPositionFactory;

  let plutus: Plutus;
  let pUsd: PlutusUsDollar;
  let pop: PlutusOptionPosition;

  beforeEach(async () => {
    [deployer] = await ethers.getSigners();

    plutusFactory = new PlutusFactory(deployer);
    pUsdFactory = new PlutusUsDollarFactory(deployer);
    popFactory = new PlutusOptionPositionFactory(deployer);

    pUsd = await pUsdFactory.deploy();
    pop = await popFactory.deploy();
    plutus = await plutusFactory.deploy(pUsd.address, pop.address);
  });

  it("sets pointers", async () => {
    expect(await plutus.POP()).to.equal(pop.address);
    expect(await plutus.pUSD()).to.equal(pUsd.address);
  });

})