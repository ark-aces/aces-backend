package io.ark.ark_client;

import com.google.common.io.BaseEncoding;
import io.ark.core.Crypto;
import lombok.RequiredArgsConstructor;
import org.bitcoinj.core.Base58;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Sha256Hash;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

@RequiredArgsConstructor
public class HttpArkClient implements ArkClient {
    
    private final ArkNetwork arkNetwork;
    private final RestTemplate restTemplate;

    @Override
    public List<Transaction> getTransactions(Integer offset) {
        return restTemplate
            .exchange(
                getRandomHostBaseUrl() + "/api/transactions?orderBy=timestamp:desc&limit=50&offset={offset}",
                HttpMethod.GET,
                null,
                TransactionsResponse.class,
                offset
            )
            .getBody()
            .getTransactions();
    }

    @Override
    public Transaction getTransaction(String id) {
        return restTemplate
            .exchange(
                getRandomHostBaseUrl() + "/api/transactions/get?id={id}",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<TransactionWrapper>() {},
                id
            ).getBody().getTransaction();
    }

    // todo: support second passphrase signing
    // todo: support different transaction types
    @Override
    public Transaction createTransaction(String recipientId, Long satoshiAmount, String vendorField, String passphrase) {
        Date beginEpoch;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            beginEpoch = dateFormat.parse("2017-03-21 13:00:00");
        } catch (ParseException e) {
            throw new RuntimeException("Failed to parse epoch start date");
        }
        long timestamp = (new Date().getTime() - beginEpoch.getTime()) / 1000L;

        CreateArkTransactionRequest createArkTransactionRequest = new CreateArkTransactionRequest();
        createArkTransactionRequest.setType((byte) 0);
        createArkTransactionRequest.setRecipientId(recipientId);
        createArkTransactionRequest.setFee(10000000L);
        createArkTransactionRequest.setVendorField(vendorField);
        createArkTransactionRequest.setTimestamp(timestamp);
        createArkTransactionRequest.setAmount(satoshiAmount);

        // sign transaction
        String senderPublicKey = BaseEncoding.base16().lowerCase().encode(Crypto.getKeys(passphrase).getPubKey());
        createArkTransactionRequest.setSenderPublicKey(senderPublicKey);

        byte[] transactionBytes = getBytes(createArkTransactionRequest, senderPublicKey);
        ECKey.ECDSASignature signature = Crypto.signBytes(transactionBytes, passphrase);
        String signatureEncoded = BaseEncoding.base16().lowerCase().encode(signature.encodeToDER());

        createArkTransactionRequest.setSignature(signatureEncoded);

        String id = BaseEncoding.base16().lowerCase().encode(Sha256Hash.hash(transactionBytes));
        createArkTransactionRequest.setId(id);

        CreateArkTransactionsRequest createArkTransactionsRequest = new CreateArkTransactionsRequest();
        createArkTransactionsRequest.setTransactions(Arrays.asList(createArkTransactionRequest));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("nethash", arkNetwork.getNetHash());
        headers.set("version", arkNetwork.getVersion());
        headers.set("port", arkNetwork.getPort());

        HttpEntity<CreateArkTransactionsRequest> requestEntity = new HttpEntity<>(createArkTransactionsRequest, headers);

        return restTemplate
            .exchange(
                getRandomHostBaseUrl() + "/peer/transactions",
                HttpMethod.POST,
                requestEntity,
                new ParameterizedTypeReference<TransactionWrapper>() {}
            ).getBody().getTransaction();
    }

    private byte[] getBytes(CreateArkTransactionRequest createArkTransactionRequest, String senderPublicKey) {
        ByteBuffer buffer = ByteBuffer.allocate(1000);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        buffer.put(createArkTransactionRequest.getType());
        buffer.putInt((int) createArkTransactionRequest.getTimestamp()); // todo: fix downcast
        buffer.put(BaseEncoding.base16().lowerCase().decode(senderPublicKey));

        if(createArkTransactionRequest.getRecipientId() != null){
            buffer.put(Base58.decodeChecked(createArkTransactionRequest.getRecipientId()));
        } else {
            buffer.put(new byte[21]);
        }

        if (createArkTransactionRequest.getVendorField() != null) {
            byte[] vbytes = createArkTransactionRequest.getVendorField().getBytes();
            if(vbytes.length < 65){
                buffer.put(vbytes);
                buffer.put(new byte[64-vbytes.length]);
            }
        } else {
            buffer.put(new byte[64]);
        }

        buffer.putLong(createArkTransactionRequest.getAmount());
        buffer.putLong(createArkTransactionRequest.getFee());

        byte[] outBuffer = new byte[buffer.position()];
        buffer.rewind();
        buffer.get(outBuffer);

        return outBuffer;
    }

    private String getRandomHostBaseUrl() {
        String httpScheme = arkNetwork.getHttpScheme();
        ArkNetworkPeer targetHost = arkNetwork.getRandomHost();
        return httpScheme + "://" + targetHost.getHostname() + ":" + targetHost.getPort();
    }
}
