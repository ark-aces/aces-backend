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
  

## Example Consumer

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



# Example

1. Start the listener application (listens on localhost:8080)

```
mvn spring-boot:run
```

2. Create a Ethereum Contract Message

```
curl -X POST 'localhost:8080/contracts' -F code=@/vagrant/data/sample.sol
```

```
{
  "token": "c7217c24-f94d-4ae1-b844-cf366cf03855",
  "serviceArkAddress": "hxuG6XABWSN7swQ6Y8ner1CYHfTLeHLH6euB52fAtW6qRcbSfA", 
  "estimatedArkCost": "1.00000000",
  "createdAt":"2017-07-01T22:35:57.695Z"
}
```

3. Create an Ark Transaction

```
var serviceArkAddress = "hxuG6XABWSN7swQ6Y8ner1CYHfTLeHLH6euB52fAtW6qRcbSfA";
var amount = 1.0000000;
var token = "c7217c24-f94d-4ae1-b844-cf366cf03855";
var transaction = ark.transaction.createTransaction(serviceArkAddress, amount, token, "passphrase", "secondPassphrase");
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
  "returnArkTransactionId": "500224999259823996",
  "ethContractAddress": "0xdaa24d02bad7e9d6a80106db164bad9399a0423e",
  "createdAt":"2017-07-01T22:35:57.695Z"
}
```
