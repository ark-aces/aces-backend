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
if (process.argv.length <= 7) {
    console.log("Usage: " + __filename + " {{ethServiceUrl}} {{walletAddress}} {{walletPassword}} " +
        "{{abiJson}} {{contractCodeBinary}} {{contractParamsJson}} {{gasLimit}}");
    process.exit(-1);
}

var Web3 = require('web3');
var ethServerUrl = process.argv[2];
var walletAddress = process.argv[3];
var walletPassword = process.argv[4];
var abi = JSON.parse(process.argv[5]);
var code = process.argv[6];
var params = JSON.parse(process.argv[7]);
var gasLimit = process.argv[8];

var web3 = new Web3();
web3.setProvider(new web3.providers.HttpProvider(ethServerUrl));

// Get the eth account and unlock
// todo: load account by address and not 0 index
var account = web3.eth.accounts[0];
web3.personal.unlockAccount(account, walletPassword);

// Look up current gasPrice or use externally provided value from the client
var gasPrice = 20000000000;

// Set up contract
var contract = web3.eth.contract(abi);
var instance = contract.new(
    ...params,
    {
        from: account,
        data: code,
        gasPrice: gasPrice,
        gas: gasLimit
    },
    function(err, contract) {
        // This callback gets called once when contract transaction is created and once when
        // it is mined. It will only have the contract.address value defined when mined.
        if (err) {
            console.log(err);
            process.exit(-1);
        } else if (contract.address) {
            // todo: find out if there is a way to get actual gas consumed after deploying contract
            var gasEstimate = web3.eth.estimateGas({data: code});

            console.log(JSON.stringify({
                "transactionHash": contract.transactionHash,
                "address": contract.address,
                "gasUsed": gasEstimate
            }));
        }
    }
);

