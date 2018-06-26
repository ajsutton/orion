package net.consensys.orion.impl.storage;

import static java.nio.charset.StandardCharsets.UTF_8;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.concurrent.AsyncResult;
import net.consensys.cava.kv.KeyValueStore;
import net.consensys.orion.api.enclave.EncryptedPayload;
import net.consensys.orion.api.storage.Storage;
import net.consensys.orion.api.storage.StorageKeyBuilder;
import net.consensys.orion.impl.enclave.sodium.SodiumEncryptedPayload;
import net.consensys.orion.impl.http.server.HttpContentType;
import net.consensys.orion.impl.utils.Base64;
import net.consensys.orion.impl.utils.Serializer;

import java.util.Optional;

public class EncryptedPayloadStorage implements Storage<EncryptedPayload> {

  private final KeyValueStore store;
  private final StorageKeyBuilder keyBuilder;

  public EncryptedPayloadStorage(KeyValueStore store, StorageKeyBuilder keyBuilder) {
    this.store = store;
    this.keyBuilder = keyBuilder;
  }

  @Override
  public AsyncResult<String> put(EncryptedPayload data) {
    String key = generateDigest(data);
    Bytes keyBytes = Bytes.wrap(key.getBytes(UTF_8));
    Bytes dataBytes = Bytes.wrap(Serializer.serialize(HttpContentType.CBOR, data));
    return store.putAsync(keyBytes, dataBytes).thenSupply(() -> key);
  }

  @Override
  public String generateDigest(EncryptedPayload data) {
    return Base64.encode(keyBuilder.build(data.cipherText()));
  }


  @Override
  public AsyncResult<Optional<EncryptedPayload>> get(String key) {
    Bytes keyBytes = Bytes.wrap(key.getBytes(UTF_8));
    return store.getAsync(keyBytes).thenApply(
        maybeBytes -> maybeBytes.map(
            bytes -> Serializer
                .deserialize(HttpContentType.CBOR, SodiumEncryptedPayload.class, bytes.toArrayUnsafe())));
  }
}
