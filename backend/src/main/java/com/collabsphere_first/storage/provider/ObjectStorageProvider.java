package com.collabsphere_first.storage.provider;

import com.collabsphere_first.storage.enums.StorageProvider;

import java.io.InputStream;

public interface ObjectStorageProvider {

    StorageProvider getProviderType();

    void storeObject(String storageKey, InputStream inputStream, String contentType, long contentLength);

    InputStream retrieveObject(String storageKey);

    boolean objectExists(String storageKey);

    long getObjectSize(String storageKey);

    void deleteObject(String storageKey);
}

