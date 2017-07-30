/**
 *
 * Copyright 2017 Paul Schaub
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
package org.jivesoftware.smackx.ciphers;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public abstract class AesGcmNoPadding {

    public static final String keyType = "AES";
    public static final String cipherMode = "AES/GCM/NoPadding";

    private final int length;
    protected final Cipher cipher;
    protected final byte[] key, iv, keyAndIv;

    public AesGcmNoPadding(int bits) throws NoSuchAlgorithmException, NoSuchProviderException,
            NoSuchPaddingException, InvalidAlgorithmParameterException, InvalidKeyException {
        this.length = bits;
        int bytes = bits / 8;

        KeyGenerator keyGenerator = KeyGenerator.getInstance(keyType);
        keyGenerator.init(bits);
        key = keyGenerator.generateKey().getEncoded();

        SecureRandom secureRandom = new SecureRandom();
        iv = new byte[bytes];
        secureRandom.nextBytes(iv);

        keyAndIv = new byte[2 * bytes];
        System.arraycopy(key, 0, keyAndIv, 0, bytes);
        System.arraycopy(iv, 0, keyAndIv, bytes, bytes);

        SecretKey secretKey = new SecretKeySpec(key, keyType);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher = Cipher.getInstance(cipherMode, "BC");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
    }

    public AesGcmNoPadding(byte[] key, byte[] iv) throws NoSuchPaddingException, NoSuchAlgorithmException,
            NoSuchProviderException, InvalidAlgorithmParameterException, InvalidKeyException {
        this.length = key.length;
        this.key = key;
        this.iv = iv;

        int bytes = length / 8;

        keyAndIv = new byte[2 * bytes];
        System.arraycopy(key, 0, keyAndIv, 0, bytes);
        System.arraycopy(iv, 0, keyAndIv, bytes, bytes);

        cipher = Cipher.getInstance(cipherMode, "BC");
        SecretKeySpec keySpec = new SecretKeySpec(key, keyType);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
    }

    public byte[] getKeyAndIv() {
        return keyAndIv.clone();
    }

    public byte[] getKey() {
        return key.clone();
    }

    public byte[] getIv() {
        return iv.clone();
    }

    public int getLength() {
        return length;
    }

    public Cipher getCipher() {
        return cipher;
    }

    public abstract String getNamespace();

    public static byte[] copyOfRange(byte[] source, int start, int end) {
        byte[] copy = new byte[end - start];
        System.arraycopy(source, start, copy, 0, end - start);
        return copy;
    }
}
