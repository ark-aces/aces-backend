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
  

## Example Consumer

The SmartBridge Listener scans Ark transactions for matching message tokens. When a matching
message is found, it sends the transaction to the consumer for further processing.

In this example, we will create an "Ark Ethereum Contract Service" that allows a user to
create an Ethereum contract via an Ark transaction.

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



