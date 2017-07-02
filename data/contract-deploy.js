var fs = require('fs');
var exec = require('child_process').execSync;

exec('solc --bin --abi --optimize -o bin contract.sol');

var abi = fs.readFileSync('bin/contract.sol:Contract.abi');
var compiled = '0x' + fs.readFileSync("bin/Contract.bin");

print(compiled);