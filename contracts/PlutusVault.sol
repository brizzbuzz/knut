// SPDX-License-Identifier: MIT
pragma solidity ^0.6.8;

import "@openzeppelin/contracts/payment/escrow/Escrow.sol";

contract PlutusVault is Escrow {

  constructor() public Escrow() {

  }

}