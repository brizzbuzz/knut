// SPDX-License-Identifier: MIT
pragma solidity ^0.7.0;

import "./PlutusVault.sol";
import "./PlutusOptionPosition.sol";
import "./PlutusUSDollar.sol";
//import "compound-open-oracle/contracts/Uniswap/UniswapAnchoredView.sol";

contract Plutus {

  // TODO Any risk with these being public?
  PlutusUsDollar public pUSD;
  PlutusOptionPosition public POP;
  PlutusVault public Vault;
//  UniswapAnchoredView public Oracle;

  // TODO Instantiate PVT Farm

  constructor(address oracleAddress) public {
    pUSD = new PlutusUsDollar();
    POP = new PlutusOptionPosition();
    Vault = new PlutusVault();
//    Oracle = UniswapAnchoredView(oracleAddress);
  }

  function lockup(address payee) public payable {
    // TODO need to multiply price * amount * mintRatio
    Vault.deposit(payee);
//    uint price = Oracle.price("ETH");
    uint price = 500;
    pUSD.mint(payee, price);
    POP.mint(payee, msg.value, price);
  }

  function exercise(uint256 optionID, address payable exerciser) public {
    require(POP.ownerOf(optionID) == exerciser, "Must be option holder to exercise");
    pUSD.burn(exerciser, POP.checkPositionCost(optionID));
    Vault.withdraw(exerciser, exerciser, POP.checkPositionValue(optionID));
    POP.burn(exerciser, optionID);
  }
}