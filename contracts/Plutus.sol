// SPDX-License-Identifier: MIT
pragma solidity ^0.6.8;

import "@nomiclabs/buidler/console.sol";
import "@openzeppelin/contracts/token/ERC20/IERC20.sol";
import "@openzeppelin/contracts/token/ERC721/IERC721.sol";
import "@openzeppelin/contracts/payment/escrow/Escrow.sol";

contract Plutus {

  IERC20 public pUSD;
  IERC721 public POP;
  Escrow public Vault;
  // Instantiate PVT Farm

  constructor(IERC20 _pUSD, IERC721 _POP, Escrow _Vault) public {
    pUSD = _pUSD;
    POP = _POP;
    Vault = _Vault;
  }

}