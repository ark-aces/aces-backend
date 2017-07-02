Vagrant.configure("2") do |config|
  config.vm.box = "ubuntu/trusty64"
  config.vm.provider "virtualbox" do |v|
    v.memory = 2048
    v.cpus = 4
  end

  config.vm.provision "shell", inline: <<-SHELL
    apt-get update
    apt-get install software-properties-common
    
    add-apt-repository -y ppa:ethereum/ethereum
    apt-get update
    apt-get install ethereum solc

    # run eth dev instance
    nohup geth --dev &

    # create an account and mine some eth
    # geth attach ipc:/tmp/ethereum_dev_mode/geth.ipc
    # 
    # personal.newAccount('12345')
    # personal.unlockAccount(eth.coinbase)

  SHELL
end
