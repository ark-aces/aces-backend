var defaultPassword = "12345";
while (web3.eth.accounts.length < 1) {
    console.log("Creating test account...");
    personal.newAccount(defaultPassword);
}

personal.unlockAccount(eth.coinbase);

miner.start(2);
admin.sleepBlocks(1);
