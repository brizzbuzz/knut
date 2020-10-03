// SPDX-License-Identifier: MIT
pragma solidity ^0.6.8;

import "@nomiclabs/buidler/console.sol";
import "@openzeppelin/contracts/token/ERC20/IERC20.sol";
import "@openzeppelin/contracts/token/ERC721/IERC721.sol";

contract Plutus {

  IERC20 public pUSD;
  IERC721 public POP;

  // Instantiate Plutus Option Position (POP) Minter
  // Instantiate Eth Vault
  // Instantiate PVT Farm

  constructor(IERC20 _pUSD, IERC721 _POP) public {
    pUSD = _pUSD;
    POP = _POP;
  }

}