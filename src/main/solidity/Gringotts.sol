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
        vault.deposit(msg.sender);
        // uint price = Oracle.price("ETH");
        uint price = 500;
        knut.mint(msg.sender, price);
        uint optionId = vows.mint(msg.sender, msg.value, price);
        emit Lockup(msg.sender, msg.value, price, optionId);
    }

    // todo Can exerciser just be sender?  how to mark payable
    function exercise(uint256 optionId, address payable exerciser) public {
        require(vows.ownerOf(optionId) == exerciser, "Must be option holder to exercise");
        knut.burn(exerciser, vows.checkPositionCost(optionId));
        vault.withdraw(exerciser, exerciser, vows.checkPositionValue(optionId));
        emit Exercise(exerciser, optionId, vows.checkPositionCost(optionId), vows.checkPositionValue(optionId));
        vows.burn(exerciser, optionId);
    }
}