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

    // TODO Make these more aligned
    event Lockup(address from, uint amount, uint value, uint optionID);
    event Exercise(address from, uint optionID, uint burned, uint value);

    // todo investigate -> Warning: Visibility for constructor is ignored
    constructor() public {
        knut = new Knut();
        vows = new UnbreakableVow();
        vault = new Vault713();
        //    Oracle = UniswapAnchoredView(oracleAddress);
    }

    function lockup() public payable {
        // TODO need to multiply price * amount * mintRatio
        // TODO can payee just be msg.sender?
        vault.deposit(msg.sender, msg.value);
        // uint price = Oracle.price("ETH");
        uint price = 500;
        knut.mint(msg.sender, price);
        uint optionId = vows.mint(msg.sender, msg.value, price);
        emit Lockup(msg.sender, msg.value, price, optionId);
    }

    // todo Can exerciser just be sender?  how to mark payable
    function exercise(uint256 optionId) public {
        emit Exercise(msg.sender, optionId, vows.checkPositionCost(optionId), vows.checkPositionValue(optionId));
        require(vows.ownerOf(optionId) == msg.sender, "Must be option holder to exercise");
        knut.burn(msg.sender, vows.checkPositionCost(optionId)); // TODO Need to verify amount available??
        vault.withdraw(msg.sender, vows.checkPositionCreator(optionId), vows.checkPositionValue(optionId));
        vows.burn(msg.sender, optionId);
    }
}