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
  decentralize this using Sia or some other distributed content network.
  

## Explanation

The SmartBridge Listener scans Ark transactions for matching message tokens. When a matching
message is found, it sends the transaction to the consumer for further processing.

In this example, we will create an "Ark Ethereum Contract Service" that allows a user to
create an Ethereum contract using via an Ark transaction.

This service will accept a block of Ethereum contract code and return a message token,
destination Ark wallet, and required Ark amount.

The client will then send an Ark transaction to the destination wallet with the message token
in the transaction SmartData field for the required Ark amount.

The Ark Transaction SmartBridge Listener will match the message token on the Ark transaction
and forward the transaction information to the Ark Ethereum Contract Service, which will
create an Ethereum contract with the matching message.

If the listener succeeds, it sends a return transaction to the client Ark wallet with any
un-used balance. 

If the listener fails, it sends a return transaction to the client with 
the original amount less transaction fees.



## Example

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
  -F abiJson=@data/solc-output/greeter.abi
  -F code=@data/solc-output/greeter.bin
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
  "returnArkTransactionId": "
  "contractTransactionHash": "500224999259823996",
  "contractAddress": "0xdaa24d02bad7e9d6a80106db164bad9399a0423e",
  "createdAt":"2017-07-01T22:35:57.695Z"
}
```


5. Running the Ethereum Contract

Load the generated Ethereum contract using the contract address in the eth console:

```
geth --dev attach
> var address = "0xdaa24d02bad7e9d6a80106db164bad9399a0423e"
> var contractInfo = admin.getContractInfo(address)
> var contract = eth.contract(contractInfo.info.abiDefinition);
> var instance = contract.at(address);
> instance
{
  abi: [{
      constant: false,
      inputs: [],
      name: "kill",
      outputs: [],
      payable: false,
      type: "function"
  }, {
      constant: true,
      inputs: [],
      name: "greet",
      outputs: [{...}],
      payable: false,
      type: "function"
  }, {
      inputs: [{...}],
      payable: false,
      type: "constructor"
  }],
  address: "0x51de2032566a2e01e9b1b7c3c3883493964815ad",
  transactionHash: "0x70825b476ade1ad2416a127cc18648e1d0926824b65397f193fd2eeedfe2c9e8",
  allEvents: function(),
  greet: function(),
  kill: function()
}
> instance.greet()
"Hello World"
```
