// SPDX-License-Identifier: MIT
pragma solidity ^0.7.0;

import "@openzeppelin/contracts/token/ERC721/ERC721.sol";
import "@openzeppelin/contracts/access/Ownable.sol";
import "@openzeppelin/contracts/utils/Counters.sol";

contract UnbreakableVow is ERC721, Ownable {

    struct Vow {
        // TODO Address
        uint256 cost;
        uint256 value;
        address creator;
    }

    using Counters for Counters.Counter;
    Counters.Counter private _tokenIds;

    mapping (uint256 => Vow) private _Vows;

    constructor() public ERC721("Unbreakable Vow", "UVOW") { }

    function mint(address payee, uint256 value, uint256 cost) public onlyOwner returns (uint256) {
        _tokenIds.increment();

        uint256 nextPopId = _tokenIds.current();

        _safeMint(payee, nextPopId);
        _setPosition(nextPopId, cost, value, payee);

        return nextPopId;
    }

    function burn(address exerciser, uint256 tokenID) public onlyOwner {
        require(ownerOf(tokenID) == exerciser, "You must own this option in order to exercise");
        delete _Vows[tokenID];
        _burn(tokenID);
    }

    function checkPosition(uint256 tokenId) public view returns (uint256, uint256, address) {
        require(_exists(tokenId), "ERC721Metadata: URI set of nonexistent token");
        // todo better to do?? -> Position memory position = _Positions[tokenId];
        return (_Vows[tokenId].value, _Vows[tokenId].cost, _Vows[tokenId].creator);
    }

    function checkPositionValue(uint256 tokenId) public view returns (uint256) {
        require(_exists(tokenId), "ERC721Metadata: URI set of nonexistent token");
        return _Vows[tokenId].value;
    }

    function checkPositionCost(uint256 tokenId) public view returns (uint256) {
        // TODO Make a modifier for this?
        require(_exists(tokenId), "ERC721Metadata: URI set of nonexistent token");
        return _Vows[tokenId].cost;
    }

    function checkPositionCreator(uint256 tokenId) public view returns (address) {
        require(_exists(tokenId), "ERC721Metadata: URI set of nonexistent token");
        return _Vows[tokenId].creator;
    }

    function _setPosition(uint256 tokenId, uint256 _cost, uint256 _value, address creator) private {
        require(_exists(tokenId), "ERC721Metadata: URI set of nonexistent token");
        _Vows[tokenId] = Vow(_cost, _value, creator);
    }

    // TODO Override and error out for unused methods?? Or is that default

}