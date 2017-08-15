/*
Usage:
nodejs eth-transaction.js {codeBinary}

Returns:
Json object containing transactionHash

Example:
TODO: Add example

Result:
{"transactionHash": "..."}
*/
if (process.argv.length <= 6) {
    console.log("Usage: " + __filename + " {{ethServiceUrl}} {{walletAddress}} {{walletPassword}} {{receiverAddress}} {{amount}}");
    process.exit(-1);
}

let Web3 = require('web3');
let ethServerUrl = process.argv[2];
let walletAddress = process.argv[3];
let walletPassword = process.argv[4];
let receiverAddress = process.argv[5];
let amount = process.argv[6];

let web3 = new Web3();
web3.setProvider(new web3.providers.HttpProvider(ethServerUrl));

// Unlock the sender's account
let account = web3.eth.accounts[0];
web3.personal.unlockAccount(account, walletPassword);
amount = web3.toWei(amount, "ether");

// Send transaction
let transaction = web3.eth.sendTransaction(
    {
        from: account,
        to: receiverAddress,
        value: amount
    },
    function(err, transactionHash) {
        if (err) {
            console.log(err);
            process.exit(-1);
        } else if (transactionHash) {
            console.log(JSON.stringify({
                "transactionHash": transactionHash,
            }));
        }
    }
);
