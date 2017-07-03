# Experimental Ark SmartBridge Listener

This is a proof-of-concept for an Ark Transaction SmartBridge listener. 


## How It Works

1. Consumer registers a message with the listener with a callback Url

2. Listener scans Ark network transactions for transactions containing your message token
   in the SmartBridge data field.
   
3. Listener sends matching transaction to registered consumer

  
## Limitations

- The SmartBridge data field is very small, so we need to store the message payload
  externally and pass only the message token in Ark transactions. 
  
- The SmartBridge Listener tokenizes messages in a centralized way. It would be nice to
  decentralize this using LBRY or some other distributed content network.
  

## Explanation

The SmartBridge Listener scans Ark transactions for matching message tokens. When a matching
message is found, it sends the transaction to the consumer for further processing.

In this example, we will create an "Ark Ethereum Contract Service" that allows a user to
create an Ethereum contract using an Ark transaction.

This service will accept a block of Ethereum contract code and return a message token,
destination Ark wallet, and required Ark amount.

The client will then send an Ark transaction of the required Ark amount to the destination wallet with the message token
in the transaction Smartbridge field.

The Ark Transaction SmartBridge Listener will match the message token on the Ark transaction
and forward the transaction information to the Ark Ethereum Contract Service, which will
create an Ethereum contract with the matching message.

If the listener succeeds, it sends a return transaction to the client Ark wallet with any
un-used balance and a message of success. 

If the listener fails, it sends a return transaction to the client with 
the original amount less transaction fees.


## Example

0. Set up environment dependencies

    The app requires ark-node to be running at on `localhost:4000`:

    ```
    app.js --genesis genesisBlock.testnet.json --config config.testnet.json
    ```
    
    This app also requires Ethereum RPC server to be running on `localhost:8545`:
    
    ```
    geth --dev --rpc --rpcapi 'web3,eth,debug' --rpccorsdomain="*"
    ```
    
    You can find server configurations in [application.yml](src/main/resources/application.yml)
   
   
1. Start the listener application (listens on localhost:8080)

    ```
    mvn spring-boot:run 
    ```

2. Create a Ethereum Contract Message

    The user will need to compile solidity contracts locally using `solc` compiler.
    This generates an `abi` and `bin` file needed to create the contract.
    
    ```
    solc --bin --abi --optimize -o data/solc-output/ data/sample.sol
    ```
    
    The user then creates a contract message with the contract data to the listener service:
    
    ```
    curl -X POST 'localhost:8080/contracts' \
        -F returnArkAddress=eijfwo91ABWSN7swQ6Y8ner1CYHfTLeHLH6euB52fAtW6qR823u20 \
        -F abiJson=@data/solc-output/greeter.abi \
        -F code=@data/solc-output/greeter.bin \
        -F params=@data/sample-params.json
    ```
    
    ```
    {
      "token": "c7217c24-f94d-4ae1-b844-cf366cf03855",
      "serviceArkAddress": "hxuG6XABWSN7swQ6Y8ner1CYHfTLeHLH6euB52fAtW6qRcbSfA", 
      "estimatedArkCost": "1.00000000",
      "returnArkAddress": "eijfwo91ABWSN7swQ6Y8ner1CYHfTLeHLH6euB52fAtW6qR823u20",
      "createdAt":"2017-07-01T22:35:57.695Z"
    }
    ```
    
    The user must send an ark transaction to the `serviceArkAddress` with at least `estimatedArkCost` ark.
    Any un-used ark will be returned to `returnArkAddress` in a ark transaction created by the service.
    

3. Create an Ark Transaction

    Using the Ark javascript API, the user sends a transaction to the `serviceArkAddress` with the
    message token in the `VendorField` parameter. The listener service will match the ark transaction
    with the corresponding contract for processing.
    
    ```
    var serviceArkAddress = "hxuG6XABWSN7swQ6Y8ner1CYHfTLeHLH6euB52fAtW6qRcbSfA";
    var messageToken = "c7217c24-f94d-4ae1-b844-cf366cf03855";
    var amount = 1.0000000;
    var transaction = ark.transaction.createTransaction(serviceArkAddress, amount, messageToken, "passphrase", "secondPassphrase");
    ```


4. Check the Status of Ethereum Contract Message

    ```
    curl -X GET 'localhost:8080/contracts/c7217c24-f94d-4ae1-b844-cf366cf03855'
    ```
    
    ```
    {
      "token": "c7217c24-f94d-4ae1-b844-cf366cf03855",
      "serviceArkAddress": "hxuG6XABWSN7swQ6Y8ner1CYHfTLeHLH6euB52fAtW6qRcbSfA", 
      "estimatedArkCost": "1.00000000",
      "returnArkAddress": "eijfwo91ABWSN7swQ6Y8ner1CYHfTLeHLH6euB52fAtW6qR823u20", 
      "actualArkCost": "0.80000000",
      "returnArkAmount": "0.2000000",
      "returnArkTransactionId": "49f55381c5c3c70f96d848df53ab7f9ae9881dbb8eb43e8f91f642018bf1258f",
      "contractTransactionHash": "500224999259823996",
      "contractAddress": "0xdaa24d02bad7e9d6a80106db164bad9399a0423e",
      "createdAt":"2017-07-01T22:35:57.695Z"
    }
    ```
    
    You can also get the original contract code:
    
    ```
    curl -X GET 'localhost:8080/contracts/c7217c24-f94d-4ae1-b844-cf366cf03855/code'
    ```
    
    ```
    6060604052341561000c57fe5b604051610309380380610309833981016040528051015b5b60008054600160a060020a03191633600160a060020a03161790555b805161005390600190602084019061005b565b505b506100fb565b828054600181600116156101000203166002900490600052602060002090601f016020900481019282601f1061009c57805160ff19168380011785556100c9565b828001600101855582156100c9579182015b828111156100c95782518255916020019190600101906100ae565b5b506100d69291506100da565b5090565b6100f891905b808211156100d657600081556001016100e0565b5090565b90565b6101ff8061010a6000396000f300606060405263ffffffff7c010000000000000000000000000000000000000000000000000000000060003504166341c0e1b58114610045578063cfae321714610057575bfe5b341561004d57fe5b6100556100e7565b005b341561005f57fe5b610067610129565b6040805160208082528351818301528351919283929083019185019080838382156100ad575b8051825260208311156100ad57601f19909201916020918201910161008d565b505050905090810190601f1680156100d95780820380516001836020036101000a031916815260200191505b509250505060405180910390f35b6000543373ffffffffffffffffffffffffffffffffffffffff908116911614156101265760005473ffffffffffffffffffffffffffffffffffffffff16ff5b5b565b6101316101c1565b60018054604080516020600284861615610100026000190190941693909304601f810184900484028201840190925281815292918301828280156101b65780601f1061018b576101008083540402835291602001916101b6565b820191906000526020600020905b81548152906001019060200180831161019957829003601f168201915b505050505090505b90565b604080516020810190915260008152905600a165627a7a72305820b154245e7f22720ca720fbac9e63d3f11289c0b6a9c0d71d46602a0f1ec41b440029
    ```
    
    ```
    curl -X GET 'localhost:8080/contracts/c7217c24-f94d-4ae1-b844-cf366cf03855/abi'
    ```
    ```
    [{"constant":false,"inputs":[],"name":"kill","outputs":[],"payable":false,"type":"function"},{"constant":true,"inputs":[],"name":"greet","outputs":[{"name":"","type":"string"}],"payable":false,"type":"function"},{"inputs":[{"name":"_greeting","type":"string"}],"payable":false,"type":"constructor"}]
    ```
    
     ```
    curl -X GET 'localhost:8080/contracts/c7217c24-f94d-4ae1-b844-cf366cf03855/params'
    ```
    
    ```
    ["Hello World"]
    ```

5. Running the Ethereum Contract

    Load the generated Ethereum contract using the contract address in the eth console:
    
    ```
    geth --dev attach
    > var contractAddress = "0xdaa24d02bad7e9d6a80106db164bad9399a0423e"
    > var contractInfo = admin.getContractInfo(contractAddress)
    > var contract = eth.contract(contractInfo.info.abiDefinition);
    > var instance = contract.at(contractAddress);
    > instance.greet()
    "Hello World"
    ```
