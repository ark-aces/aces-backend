var Web3 = require('web3');

if (process.argv.length <= 4) {
    console.log("Usage: " + __filename + " {{abiJson}} {{contractCodeBinary}} {{contractParamsJson}}");
    process.exit(-1);
}

var abi = JSON.parse(process.argv[2]);
var code = process.argv[3];
var params = JSON.parse(process.argv[4]);

var web3 = new Web3();
// todo: externalize rpc provider information, this is currently hard coded to test values
web3.setProvider(new web3.providers.HttpProvider('http://localhost:8545'));

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
            process.exit(-1);
        } else if (contract.address) {
            console.log({
                "transactionHash": contract.transactionHash,
                "address": contract.address
            });
            process.exit(0);
        }
    }
);

