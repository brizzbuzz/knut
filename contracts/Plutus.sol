// SPDX-License-Identifier: MIT
pragma solidity ^0.6.8;

import "@nomiclabs/buidler/console.sol";

import "./PlutusVault.sol";
import "./PlutusOptionPosition.sol";
import "./PlutusUsDollar.sol";

contract Plutus {

  PlutusUsDollar public pUSD;
  PlutusOptionPosition public POP;
  PlutusVault public Vault;
  // Instantiate PVT Farm

  constructor() public {
    pUSD = new PlutusUsDollar();
    POP = new PlutusOptionPosition();
    Vault = new PlutusVault();
  }

  function lockup(address payee) public payable {
    Vault.deposit(payee);
    pUSD.mint(payee, 500);
    POP.mint(payee, msg.value, 500);
  }

  function exercise(uint256 optionID, address payable exerciser) public {
    require(POP.ownerOf(optionID) == exerciser, "Must be option holder to exercise");
    pUSD.burn(exerciser, POP.checkPositionCost(optionID));
    Vault.withdraw(exerciser, exerciser, POP.checkPositionValue(optionID));
    POP.burn(exerciser, optionID);
  }
}