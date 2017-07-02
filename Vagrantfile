Vagrant.configure("2") do |config|
  config.vm.box = "ubuntu/trusty64"
  config.vm.provider "virtualbox" do |v|
    v.memory = 2048
    v.cpus = 4
  end

  config.vm.provision "shell", inline: <<-SHELL
    apt-get update
    apt-get install software-properties-common build-essential
    
    add-apt-repository -y ppa:ethereum/ethereum
    apt-get update
    apt-get install ethereum solc

    curl -sL https://deb.nodesource.com/setup_6.x | sudo -E bash -
    sudo apt-get install nodejs

    # run eth dev instance
    nohup geth --dev --rpc --rpcapi 'web3,eth,debug' --rpccorsdomain="*" &

    # create an account and mine some eth using 
    geth --dev --preload /vagrant/bin/preload.js attach ipc:/tmp/ethereum_dev_mode/geth.ipc

  SHELL
end
