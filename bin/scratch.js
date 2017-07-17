var ark = require('arkjs');

var keys = ark.crypto.getKeys("12345");

var address = ark.crypto.getAddress(keys['publicKey']);

console.log("address: " + address);
