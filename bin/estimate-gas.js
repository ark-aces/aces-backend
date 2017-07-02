/*
Usage:
nodejs estimate-gas.js {ethServer} {codeBinary}

Returns:
Json object containing gasEstimate property

Example:
nodejs estimate-gas.js 'http://localhost:8545' 0x6060604052341561000c57fe5b{...}

Result:
{"gasEstimate":229411}
 */
if (process.argv.length <= 3) {
    console.log("Usage: nodejs estimate-gas.js {{ethServer}} {{contractCodeBinary}}");
    console.log("params: " + process.argv);
    process.exit(-1);
}

var Web3 = require('web3');
var ethServerUrl = process.argv[2];
var code = process.argv[3];

var web3 = new Web3();
web3.setProvider(new web3.providers.HttpProvider(ethServerUrl));
var gasEstimate = web3.eth.estimateGas({data: code});

console.log(JSON.stringify({
    "gasEstimate": gasEstimate
}));
