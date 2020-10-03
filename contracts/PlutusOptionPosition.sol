// SPDX-License-Identifier: MIT
pragma solidity ^0.6.8;

import "@openzeppelin/contracts/token/ERC721/ERC721.sol";

contract PlutusOptionPosition is ERC721 {

  constructor() public ERC721("Plutus Option Position", "POP") {

  }

}