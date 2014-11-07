/*
 * Copyright 2014 Inductive Automation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.inductiveautomation.opcua.server.ctt;

import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.EnumSet;
import java.util.List;

import com.google.common.collect.Lists;
import com.inductiveautomation.opcua.sdk.server.api.OpcUaServerConfig;
import com.inductiveautomation.opcua.sdk.server.identity.IdentityValidator;
import com.inductiveautomation.opcua.sdk.server.identity.UsernameIdentityValidator;
import com.inductiveautomation.opcua.stack.core.security.SecurityPolicy;
import com.inductiveautomation.opcua.stack.core.types.builtin.LocalizedText;
import com.inductiveautomation.opcua.stack.core.types.structured.UserTokenPolicy;
import org.slf4j.LoggerFactory;

public class CttServerConfig implements OpcUaServerConfig {

    private static final String SERVER_ALIAS = "ctt-server-certificate3";
    private static final char[] PASSWORD = "test".toCharArray();

    private volatile Certificate certificate;
    private volatile KeyPair keyPair;

    public CttServerConfig() {
        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");

            keyStore.load(getClass().getClassLoader().getResourceAsStream("ctt-server-keystore.pfx"), PASSWORD);

            Key serverPrivateKey = keyStore.getKey(SERVER_ALIAS, PASSWORD);

            if (serverPrivateKey instanceof PrivateKey) {
                certificate = keyStore.getCertificate(SERVER_ALIAS);
                PublicKey serverPublicKey = certificate.getPublicKey();
                keyPair = new KeyPair(serverPublicKey, (PrivateKey) serverPrivateKey);
            }
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("Error loading server certificate: {}.", e.getMessage(), e);

            certificate = null;
            keyPair = null;
        }
    }

    @Override
    public LocalizedText getApplicationName() {
        return LocalizedText.english("Compliance Testing OPC-UA Server");
    }

    @Override
    public String getApplicationUri() {
        return "urn:inductiveautomation:ctt-server";
    }

    @Override
    public String getProductUri() {
        return "http://www.inductiveautomation.com/opc-ua/sdk";
    }

    @Override
    public String getServerName() {
        return "ctt-server";
    }

    @Override
    public KeyPair getKeyPair() {
        return keyPair;
    }

    @Override
    public Certificate getCertificate() {
        return certificate;
    }

    @Override
    public EnumSet<SecurityPolicy> getSecurityPolicies() {
        return EnumSet.of(SecurityPolicy.None, SecurityPolicy.Basic128Rsa15);
    }

    @Override
    public List<UserTokenPolicy> getUserTokenPolicies() {
        return Lists.newArrayList(USER_TOKEN_POLICY_ANONYMOUS, USER_TOKEN_POLICY_USERNAME);
    }

    @Override
    public IdentityValidator getIdentityValidator() {
        return new UsernameIdentityValidator(true, challenge -> {
            String username = challenge.getUsername();
            String password = challenge.getPassword();

            return ("user1".equals(username) && "password1".equals(password)) ||
                    ("user2".equals(username) && "password2".equals(password));
        });
    }

}
