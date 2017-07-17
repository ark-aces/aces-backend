Vagrant.configure("2") do |config|
  config.vm.box = "ubuntu/trusty64"
  config.vm.provider "virtualbox" do |v|
    v.memory = 2048
    v.cpus = 4
  end

  config.vm.network "forwarded_port", guest: 8545, host: 8545

  config.vm.provision "shell", inline: <<-SHELL
    apt-get update
    apt-get install software-properties-common build-essential
    
    add-apt-repository -y ppa:ethereum/ethereum
    apt-get update
    apt-get install ethereum solc

    add-apt-repository ppa:webupd8team/java -y
    apt-get update
    echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | sudo /usr/bin/debconf-set-selections
    apt-get install oracle-java8-installer

    curl -sL https://deb.nodesource.com/setup_6.x | sudo -E bash -
    sudo apt-get install nodejs

    # run eth dev instance
    nohup geth --dev --rpc --rpcaddr=0.0.0.0 --rpcapi 'web3,eth,debug' --rpccorsdomain="*" &

    # create an account and mine some eth using 
    geth --dev --preload /vagrant/bin/preload.js attach ipc:/tmp/ethereum_dev_mode/geth.ipc

  SHELL
end
