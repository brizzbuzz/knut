// SPDX-License-Identifier: MIT
pragma solidity ^0.7.0;

import "./Vault713.sol";
import "./UnbreakableVow.sol";
import "./Knut.sol";
//import "compound-open-oracle/contracts/Uniswap/UniswapAnchoredView.sol";

contract Gringotts {

    // TODO Any risk with these being public?
    Knut public knut;
    UnbreakableVow public vows;
    Vault713 public vault;
    //  UniswapAnchoredView public Oracle;

    // TODO Instantiate Incentive Token

    constructor(address oracleAddress) public {
        knut = new Knut();
        vows = new UnbreakableVow();
        vault = new Vault713();
        //    Oracle = UniswapAnchoredView(oracleAddress);
    }

    function lockup(address payee) public payable {
        // TODO need to multiply price * amount * mintRatio
        vault.deposit(payee);
        //    uint price = Oracle.price("ETH");
        uint price = 500;
        knut.mint(payee, price);
        vows.mint(payee, msg.value, price);
    }

    function exercise(uint256 optionID, address payable exerciser) public {
        require(vows.ownerOf(optionID) == exerciser, "Must be option holder to exercise");
        knut.burn(exerciser, vows.checkPositionCost(optionID));
        vault.withdraw(exerciser, exerciser, vows.checkPositionValue(optionID));
        vows.burn(exerciser, optionID);
    }
}