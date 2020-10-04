// SPDX-License-Identifier: MIT
pragma solidity ^0.6.8;

import "@nomiclabs/buidler/console.sol";
import "@openzeppelin/contracts/token/ERC20/IERC20.sol";
import "@openzeppelin/contracts/token/ERC721/IERC721.sol";
import "@openzeppelin/contracts/payment/escrow/Escrow.sol";

import "./PlutusVault.sol";
import "./PlutusOptionPosition.sol";
import "./PlutusUsDollar.sol";

contract Plutus {

  PlutusUsDollar public pUSD;
  IERC721 public POP;
  Escrow public Vault;
  // Instantiate PVT Farm

  constructor() public {
    pUSD = new PlutusUsDollar();
    POP = new PlutusOptionPosition();
    Vault = new PlutusVault();
  }

  function lockup(address payee) public payable {
    Vault.deposit(payee);
    pUSD.mint(payee, 500);
    // todo mint pop
  }
}