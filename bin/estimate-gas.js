var Web3 = require('web3');

if (process.argv.length <= 2) {
    console.log("Usage: " + __filename + " 0x{{contractCodeBinary}}");
    process.exit(-1);
}

var code = process.argv[2];

var web3 = new Web3();
web3.setProvider(new web3.providers.HttpProvider('http://localhost:8545'));
var gasEstimate = web3.eth.estimateGas({data: code});

console.log(gasEstimate);
