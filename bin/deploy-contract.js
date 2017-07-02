/*
Usage:
nodejs estimate-gas.js {codeBinary}

Returns:
Json object containing transactionHash and address properties

Example:
nodejs deploy-contract.js \
'http://localhost:8545' \
'[{"constant":false,"inputs":[],"name":"kill",{....}'
0x6060604052341561000c57fe5b6{...} \
'["Hello World"]'

Result:
{"transactionHash": "...", "address": "...."}
 */
// todo: figure out how long this script might take to run against real eth network since
// it waits for contract transactions to be mined before returning address
if (process.argv.length <= 5) {
    console.log("Usage: " + __filename + " {{abiJson}} {{contractCodeBinary}} {{contractParamsJson}}");
    process.exit(-1);
}

var Web3 = require('web3');

var ethServerUrl = process.argv[2];
var abi = JSON.parse(process.argv[3]);
var code = process.argv[4];
var params = JSON.parse(process.argv[5]);

var web3 = new Web3();
web3.setProvider(new web3.providers.HttpProvider(ethServerUrl));

var contract = web3.eth.contract(abi);

// todo: externalize account information, this is currently hard coded to test values
web3.personal.unlockAccount(web3.eth.accounts[0], "12345");

// todo: figure out what gas limit should be, or pass it in as a param
var instance = contract.new(
    ...params,
    {
        from: web3.eth.accounts[0],
        data: code,
        gas: 1000000
    },
    function(err, contract) {
        // This callback gets called once when contract transaction is created and once when
        // it is mined. It will only have the contract.address value defined when mined.
        if (err) {
            console.log(err);
            process.exit(-1);
        } else if (contract.address) {
            console.log(JSON.stringify({
                "transactionHash": contract.transactionHash,
                "address": contract.address
            }));
        }
    }
);

