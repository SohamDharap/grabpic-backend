package com.grabpic.backend.service.impl;

import com.google.auth.ServiceAccountSigner;
import com.google.cloud.iam.credentials.v1.IamCredentialsClient;
import com.google.cloud.iam.credentials.v1.ServiceAccountName;
import com.google.cloud.iam.credentials.v1.SignBlobResponse;
import com.google.protobuf.ByteString;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;

/**
 * ServiceAccountSigner implementation that uses GCP IAM Credentials API (signBlob)
 * to sign V4 URLs using Application Default Credentials (ADC) on Compute Engine
 * without requiring any local service account private key JSON file.
 */
@RequiredArgsConstructor
@Slf4j
public class IamServiceAccountSigner implements ServiceAccountSigner {

    private final IamCredentialsClient iamCredentialsClient;
    private final String serviceAccountEmail;

    @Override
    public String getAccount() {
        return serviceAccountEmail;
    }

    @Override
    public byte[] sign(byte[] toSign) {
        log.debug("Requesting IAM Credentials signBlob for service account: {}", serviceAccountEmail);
        ServiceAccountName name = ServiceAccountName.of("-", serviceAccountEmail);
        SignBlobResponse response = iamCredentialsClient.signBlob(
                name,
                Collections.emptyList(),
                ByteString.copyFrom(toSign)
        );
        return response.getSignedBlob().toByteArray();
    }
}
