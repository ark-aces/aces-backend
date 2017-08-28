/*
Usage:
nodejs get-balance.js {ethServer} {walletAddress}

Returns:
Json object containing gasEstimate property

Example:
nodejs get-balance.js 'http://localhost:8545' 0x6060604052341561000c57fe5b

Result:
{"balance":1}
 */

if (process.argv.length <= 3) {
    console.log("Usage: nodejs get-balance.js {{ethServer}} {{ethWalletAddress}}");
    console.log("params: " + process.argv);
    process.exit(-1);
}

var Web3 = require('web3');
var ethServerUrl = process.argv[2];
var walletAddress = process.argv[3];

var web3 = new Web3();
web3.setProvider(new web3.providers.HttpProvider(ethServerUrl));

var balance = 0;
for (var i = 0; i < web3.eth.accounts.length; i++) {
    var account = web3.eth.accounts[i];
    if (account === walletAddress) {
        balance = web3.fromWei(web3.eth.getBalance(account), "ether");
    }
}
console.log(JSON.stringify({
    "balance": balance
}));
