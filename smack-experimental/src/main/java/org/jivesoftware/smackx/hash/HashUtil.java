/**
 *
 * Copyright © 2017 Paul Schaub
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
package org.jivesoftware.smackx.hash;

import org.bouncycastle.jcajce.provider.digest.Blake2b;
import org.bouncycastle.jcajce.provider.digest.SHA224;
import org.bouncycastle.jcajce.provider.digest.SHA256;
import org.bouncycastle.jcajce.provider.digest.SHA3;
import org.bouncycastle.jcajce.provider.digest.SHA384;
import org.bouncycastle.jcajce.provider.digest.SHA512;
import org.jivesoftware.smack.util.MD5;
import org.jivesoftware.smack.util.SHA1;

import java.math.BigInteger;

/**
 * Class that provides hash functionality.
 */
public final class HashUtil {

    public enum ALGORITHM {
        MD5 ("md5"),
        SHA_1 ("sha-1"),
        SHA_224 ("sha-224"),
        SHA_256 ("sha-256"),
        SHA_384 ("sha-384"),
        SHA_512 ("sha-512"),
        SHA3_224 ("sha3-224"),
        SHA3_256 ("sha3-256"),
        SHA3_384 ("sha3-384"),
        SHA3_512 ("sha3-512"),
        ID_BLAKE2B160 ("id-blake2b160"),
        ID_BLAKE2B256 ("id-blake2b256"),
        ID_BLAKE2B384 ("id-blake2b384"),
        ID_BLAKE2B512 ("id-blake2b512");

        private final String name;

        ALGORITHM(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return this.name;
        }

        public static ALGORITHM get(String s) {
            for (ALGORITHM a : ALGORITHM.values()) {
                if (s.equals(a.toString())) {
                    return a;
                }
            }
            throw new IllegalArgumentException("No ALGORITHM enum with this name (" + s + ") found.");
        }
    }

    public static byte[] hash(ALGORITHM algorithm, byte[] data) {
        byte[] hash;
        switch (algorithm) {
            case MD5:
                hash = md5(data);
                break;
            case SHA_1:
                hash = sha_1(data);
                break;
            case SHA_224:
                hash = sha_224(data);
                break;
            case SHA_256:
                hash = sha_256(data);
                break;
            case SHA_384:
                hash = sha_384(data);
                break;
            case SHA_512:
                hash = sha_512(data);
                break;
            case SHA3_224:
                hash = sha3_224(data);
                break;
            case SHA3_256:
                hash = sha3_256(data);
                break;
            case SHA3_384:
                hash = sha3_384(data);
                break;
            case SHA3_512:
                hash = sha3_512(data);
                break;
            case ID_BLAKE2B160:
                hash = id_blake2b160(data);
                break;
            case ID_BLAKE2B256:
                hash = id_blake2b256(data);
                break;
            case ID_BLAKE2B384:
                hash = id_blake2b384(data);
                break;
            case ID_BLAKE2B512:
                hash = id_blake2b512(data);
                break;
            default:
                throw new AssertionError("Invalid enum value.");
        }
        return hash;
    }

    public static byte[] md5(byte[] data) {
        return MD5.bytes(data);
    }

    public static byte[] sha_1(byte[] data) {
        return SHA1.bytes(data);
    }

    public static byte[] sha_224(byte[] data) {
        return new SHA224.Digest().digest(data);
    }

    public static byte[] sha_256(byte[] data) {
        return new SHA256.Digest().digest(data);
    }

    public static byte[] sha_384(byte[] data) {
        return new SHA384.Digest().digest(data);
    }

    public static byte[] sha_512(byte[] data) {
        return new SHA512.Digest().digest(data);
    }

    public static byte[] sha3_224(byte[] data) {
        return new SHA3.Digest224().digest(data);
    }

    public static byte[] sha3_256(byte[] data) {
        return new SHA3.Digest256().digest(data);
    }

    public static byte[] sha3_384(byte[] data) {
        return new SHA3.Digest384().digest(data);
    }

    public static byte[] sha3_512(byte[] data) {
        return new SHA3.Digest512().digest(data);
    }

    public static byte[] id_blake2b160(byte[] data) {
        return new Blake2b.Blake2b160().digest(data);
    }

    public static byte[] id_blake2b256(byte[] data) {
        return new Blake2b.Blake2b256().digest(data);
    }

    public static byte[] id_blake2b384(byte[] data) {
        return new Blake2b.Blake2b384().digest(data);
    }

    public static byte[] id_blake2b512(byte[] data) {
        return new Blake2b.Blake2b512().digest(data);
    }

    public static String hex(byte[] hash) {
        return new BigInteger(1, hash).toString(16);
    }
}
