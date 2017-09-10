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
  decentralize this using IPFS or some other distributed content network.
  

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
    geth --dev --rpc --rpcapi 'web3,eth,debug,personal' --rpccorsdomain="*"
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
        -F returnArkAddress=ARNJJruY6RcuYCXcwWsu4bx9kyZtntqeAx \
        -F abiJson=@data/solc-output/greeter.abi \
        -F code=@data/solc-output/greeter.bin \
        -F params=@data/sample-params.json \
        -F gasLimit=500000
    ```
    
    ```
    {
      "token" : "abe05cd7-40c2-4fb0-a4a7-8d2f76e74978",
      "createdAt" : "2017-07-04T21:59:38.129Z",
      "estimatedGasCost" : "214411",
      "estimatedArkCost" : "1.00000000",
      "serviceArkAddress": "AewU1vEmPrtQNjdVo33cX84bfovY3jNAkV", 
      "returnArkAddress" : "ARNJJruY6RcuYCXcwWsu4bx9kyZtntqeAx",
      "contractCode" : "0x6060604052341561000c57fe5b604051610309380380610309833981016040528051015b5b60008054600160a060020a03191633600160a060020a03161790555b805161005390600190602084019061005b565b505b506100fb565b828054600181600116156101000203166002900490600052602060002090601f016020900481019282601f1061009c57805160ff19168380011785556100c9565b828001600101855582156100c9579182015b828111156100c95782518255916020019190600101906100ae565b5b506100d69291506100da565b5090565b6100f891905b808211156100d657600081556001016100e0565b5090565b90565b6101ff8061010a6000396000f300606060405263ffffffff7c010000000000000000000000000000000000000000000000000000000060003504166341c0e1b58114610045578063cfae321714610057575bfe5b341561004d57fe5b6100556100e7565b005b341561005f57fe5b610067610129565b6040805160208082528351818301528351919283929083019185019080838382156100ad575b8051825260208311156100ad57601f19909201916020918201910161008d565b505050905090810190601f1680156100d95780820380516001836020036101000a031916815260200191505b509250505060405180910390f35b6000543373ffffffffffffffffffffffffffffffffffffffff908116911614156101265760005473ffffffffffffffffffffffffffffffffffffffff16ff5b5b565b6101316101c1565b60018054604080516020600284861615610100026000190190941693909304601f810184900484028201840190925281815292918301828280156101b65780601f1061018b576101008083540402835291602001916101b6565b820191906000526020600020905b81548152906001019060200180831161019957829003601f168201915b505050505090505b90565b604080516020810190915260008152905600a165627a7a72305820b154245e7f22720ca720fbac9e63d3f11289c0b6a9c0d71d46602a0f1ec41b440029",
      "contractAbiJson" : "[{\"constant\":false,\"inputs\":[],\"name\":\"kill\",\"outputs\":[],\"payable\":false,\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"greet\",\"outputs\":[{\"name\":\"\",\"type\":\"string\"}],\"payable\":false,\"type\":\"function\"},{\"inputs\":[{\"name\":\"_greeting\",\"type\":\"string\"}],\"payable\":false,\"type\":\"constructor\"}]",
      "contractParamsJson" : "[\"Hello World\"]"
    }
    ```
    
    The user must send an ark transaction to the `serviceArkAddress` with at least `estimatedArkCost` ark.
    Any un-used ark will be returned to `returnArkAddress` in a ark transaction created by the service.
    

3. Create an Ark Transaction

    Using the Ark javascript API, the user sends a transaction to the `serviceArkAddress` with the
    message token in the `VendorField` parameter. The listener service will match the ark transaction
    with the corresponding contract for processing.
    
    ```
    var serviceArkAddress = "AewU1vEmPrtQNjdVo33cX84bfovY3jNAkV";
    var messageToken = "abe05cd7-40c2-4fb0-a4a7-8d2f76e74978";
    var amount = 1.0000000;
    var transaction = ark.transaction.createTransaction(serviceArkAddress, amount, messageToken, "passphrase", "secondPassphrase");
    ```


4. Check the Status of Ethereum Contract Message

    ```
    curl -X GET 'localhost:8080/contracts/abe05cd7-40c2-4fb0-a4a7-8d2f76e74978'
    ```
    
    ```
    {
      "token" : "abe05cd7-40c2-4fb0-a4a7-8d2f76e74978",
      "createdAt" : "2017-07-04T21:59:38.129Z",
      "estimatedGasCost" : "214411.00",
      "estimatedArkCost" : "1.00",
      "serviceArkAddress": "AewU1vEmPrtQNjdVo33cX84bfovY3jNAkV", 
      "returnArkAddress" : "ARNJJruY6RcuYCXcwWsu4bx9kyZtntqeAx",
      "contractCode" : "0x6060604052341561000c57fe5b604051610309380380610309833981016040528051015b5b60008054600160a060020a03191633600160a060020a03161790555b805161005390600190602084019061005b565b505b506100fb565b828054600181600116156101000203166002900490600052602060002090601f016020900481019282601f1061009c57805160ff19168380011785556100c9565b828001600101855582156100c9579182015b828111156100c95782518255916020019190600101906100ae565b5b506100d69291506100da565b5090565b6100f891905b808211156100d657600081556001016100e0565b5090565b90565b6101ff8061010a6000396000f300606060405263ffffffff7c010000000000000000000000000000000000000000000000000000000060003504166341c0e1b58114610045578063cfae321714610057575bfe5b341561004d57fe5b6100556100e7565b005b341561005f57fe5b610067610129565b6040805160208082528351818301528351919283929083019185019080838382156100ad575b8051825260208311156100ad57601f19909201916020918201910161008d565b505050905090810190601f1680156100d95780820380516001836020036101000a031916815260200191505b509250505060405180910390f35b6000543373ffffffffffffffffffffffffffffffffffffffff908116911614156101265760005473ffffffffffffffffffffffffffffffffffffffff16ff5b5b565b6101316101c1565b60018054604080516020600284861615610100026000190190941693909304601f810184900484028201840190925281815292918301828280156101b65780601f1061018b576101008083540402835291602001916101b6565b820191906000526020600020905b81548152906001019060200180831161019957829003601f168201915b505050505090505b90565b604080516020810190915260008152905600a165627a7a72305820b154245e7f22720ca720fbac9e63d3f11289c0b6a9c0d71d46602a0f1ec41b440029",
      "contractAbiJson" : "[{\"constant\":false,\"inputs\":[],\"name\":\"kill\",\"outputs\":[],\"payable\":false,\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"greet\",\"outputs\":[{\"name\":\"\",\"type\":\"string\"}],\"payable\":false,\"type\":\"function\"},{\"inputs\":[{\"name\":\"_greeting\",\"type\":\"string\"}],\"payable\":false,\"type\":\"constructor\"}]",
      "contractParamsJson" : "[\"Hello World\"]"
      "actualArkCost": "0.80000000",
      "returnArkAmount": "0.2000000",
      "returnArkTransactionId": "49f55381c5c3c70f96d848df53ab7f9ae9881dbb8eb43e8f91f642018bf1258f",
      "contractTransactionHash": "500224999259823996",
      "contractAddress": "0xdaa24d02bad7e9d6a80106db164bad9399a0423e",
      "createdAt":"2017-07-01T22:35:57.695Z"
    }
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


## Deploying your Node

These steps help you get set up a Ubuntu 16.04 linux server running ACES. You'll need at least 50GB of disk
space to sync the ethereum blockchain (might not be necessary for light syncing).

These instructions use systemd to supervise applications running in the background and nginx to serve
web traffic.

1. Install system dependencies

```
sudo apt-get python-software-properties
sudo add-apt-repository -y ppa:ethereum/ethereum
sudo add-apt-repository ppa:webupd8team/java -y

sudo apt-get update
sudo apt-get install software-properties-common build-essential curl git maven
sudo apt-get install ethereum solc

# install Java JVM
echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | sudo /usr/bin/debconf-set-selections
sudo apt-get install oracle-java8-installer

# install nodejs
curl -sL https://deb.nodesource.com/setup_6.x | sudo -E bash -
sudo apt-get install nodejs

# install nginx
sudo apt-get install nginx
```

    
2. Install ethereum service

Copy the following into `/etc/systemd/system/geth-network.service`. There are 3 different ways to run geth to
target different ethereum networks: dev, testnet, and mainnet.

Local Devnet:

```
[Unit]
Description=Ethereum devnet

[Service]
Restart=always
ExecStart=/usr/bin/geth --dev --rpc --rpcaddr=127.0.0.1 --rpcapi 'web3,eth,personal' --rpccorsdomain="*" \
--datadir=/eth-data
[Install]
WantedBy=multi-user.target
```

Ropsten Testnet:

```
[Unit]
Description=Ethereum testnet

[Service]
Restart=always
ExecStart=/usr/bin/geth --testnet --rpc --rpcaddr=127.0.0.1 --rpcapi 'web3,eth,personal' --rpccorsdomain="*" \
--datadir=/eth-data --fast --cache=1024 \
--bootnodes "enode://20c9ad97c081d63397d7b685a412227a40e23c8bdc6688c6f37e97cfbc22d2b4d1db1510d8f61e6a8866ad7f0e17c02b14182d37ea7c3c8b9c2683aeb6b733a1@52.169.14.227:30303,enode://6ce05930c72abc632c58e2e4324f7c7ea478cec0ed4fa2528982cf34483094e9cbc9216e7aa349691242576d552a2a56aaeae426c5303ded677ce455ba1acd9d@13.84.180.240:30303"

[Install]
WantedBy=multi-user.target
```

Mainnet:

```
[Unit]
Description=Ethereum network

[Service]
Restart=always
ExecStart=/usr/bin/geth --rpc --rpcaddr=127.0.0.1 --rpcapi 'web3,eth,personal' --rpccorsdomain="*" \
--datadir=/eth-data --fast --cache=1024 \
--bootnodes "enode://20c9ad97c081d63397d7b685a412227a40e23c8bdc6688c6f37e97cfbc22d2b4d1db1510d8f61e6a8866ad7f0e17c02b14182d37ea7c3c8b9c2683aeb6b733a1@52.169.14.227:30303,enode://6ce05930c72abc632c58e2e4324f7c7ea478cec0ed4fa2528982cf34483094e9cbc9216e7aa349691242576d552a2a56aaeae426c5303ded677ce455ba1acd9d@13.84.180.240:30303"

[Install]
WantedBy=multi-user.target
```


Start up ethereum service (this can take several hours to sync before transactions):

```
sudo chmod 655 /etc/systemd/system/geth-network.service
sudo systemctl daemon-reload
sudo service geth-network start
```

You can view the output of running systemd services by tailing the syslog:

```
sudo tail -f /var/log/syslog
```

3. Create Service Eth Wallet

Connect to the local geth instance and create a new eth wallet for ACES service:

```
geth attach ipc:/eth-data/geth.ipc

> let password = '12345';
> personal.newAccount(password);
> eth.accounts
["0x77fa2ae66ff74d99da00cdd3f82a3a50750bb95d"]
```

Use the eth wallet address and password in the application.yml configuration below.

4. Install ACES application

Install the application under `/apps`:

```
sudo mkdir /apps
sudo mkdir /apps/aces-backend
cd /apps/aces-backend
git clone https://github.com/bradyo/ark-java-smart-bridge-listener.git
```

Install application.yml configuration file under `/etc/aces-backend`:

```
mkdir /etc/aces-backend
```

Copy `src/main/resources/application.yml` file over into `/etc/aces-backend` and replace
wallet configurations with your own Eth and Ark wallet addresses and passphrase.

```
spring:
  datasource:
    url: "jdbc:h2:/tmp/testdb;DB_CLOSE_ON_EXIT=FALSE;AUTO_RECONNECT=TRUE"
    driver-class-name: "org.h2.Driver"
  jpa:
    hibernate:
      ddl-auto: "update"

arkNetwork:
  name: "mainnet"

ethBridge:
  ethServerUrl: "http://localhost:8545"
  scriptPath: "./bin"
  nodeCommand: "/usr/bin/node"

  serviceArkWallet:
    address: "change me"
    passphrase: "change me"
    passphrase2:

  serviceEthAccount:
    address: "change me"
    password: "change me"
    
arkPerEthAdjustment: "100.00"

ethContractDeployService:
  arkFlatFee: "2.00"
  arkPercentFee: "2.25"
  requiredArkMultiplier: "1.2"

ethTransferService:
  arkFlatFee: "1.00"
  arkPercentFee: "1.25"
  requiredArkMultiplier: "1"
```

For production instances, you should configure the application to use a real database like `posgresql`:

```
spring:
  datasource:
    url: "jdbc:postgresql://{host}:{port}/{database_name}"
    username: "change me"
    password: "change me"
  jpa:
    database-platform: "org.hibernate.dialect.PostgreSQLDialect"
    generate-ddl: true
    hibernate:
      ddl-auto: "update"
```

Copy the following into `/etc/systemd/system/aces-backend.service`:

```
[Unit]
Description=Aces Backend

[Service]
Restart=always
WorkingDirectory=/apps/aces-backend/ark-java-smart-bridge-listener
ExecStart=/usr/bin/mvn spring-boot:run -Dspring.config.location=file:/etc/aces-backend/application.yml

[Install]
WantedBy=multi-user.target
```

Install npm dependencies:

```
cd /apps/aces-backend/ark-java-smart-bridge-listener/bin
npm install
```


Start the ACES backend service:

```
sudo chmod 655 /etc/systemd/system/aces-backend.service
sudo systemctl daemon-reload
sudo service aces-backend start
```

5. Installing ACES frontend

Install the application under `/apps`:

```
sudo mkdir /apps/aces-frontend
cd /apps/aces-frontend
git clone https://github.com/bradyo/aces-app.git
```

todo: set up api url via config and point that to the backend-api
https://github.com/bradyo/aces-app/blob/master/src/app/aces-server-config.ts#L8


Build the frontend application:

```
cd /apps/aces-frontend/aces-app
npm install -g @angular/cli
npm install
ng build --target=production --base-href /aces-app/
```

6. Set up nginx web server

Allow backend and frontend to be served by nginx by adding the following to `/etc/nginx/sites-available/default`
under root `server` directive.

```
location /aces-app/ {
    alias /apps/aces-frontend/aces-app/dist/;
    try_files $uri $uri/ /aces-app/;
}

location /aces-api/ {
    proxy_pass http://127.0.0.1:8080/;
}
```

```
sudo service nginx restart
```

7. Confirm setup

Check the backend API by making a GET request to `http://localhost/aces-api/test-service-info`:

```
curl http://localhost/aces-api/test-service-info
{
  "capacity" : "∞",
  "flatFeeArk" : "0",
  "percentFee" : "0",
  "status" : "Up"
}
```

Open up the aces-app frontend in your browser: `https://localhost/aces-app/`
