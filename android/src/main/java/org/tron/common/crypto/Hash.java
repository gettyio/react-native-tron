/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */

package org.tron.common.crypto;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.bouncycastle.crypto.digests.RIPEMD160Digest;
import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.jcajce.provider.digest.Keccak;

/**
 * Cryptographic hash functions.
 */
public class Hash {

  private Hash() { }

  /**
   * Keccak-256 hash function.
   *
   * @param hexInput hex encoded input data with optional 0x prefix
   * @return hash value as hex encoded string
   */
  public static String sha3(String hexInput) {
    byte[] bytes = Numeric.hexStringToByteArray(hexInput);
    byte[] result = sha3(bytes);
    return Numeric.toHexString(result);
  }

  /**
   * Keccak-256 hash function.
   *
   * @param input binary encoded input data
   * @param offset of start of data
   * @param length of data
   * @return hash value
   */
  public static byte[] sha3(byte[] input, int offset, int length) {
    Keccak.DigestKeccak kecc = new Keccak.Digest256();
    kecc.update(input, offset, length);
    return kecc.digest();
  }

  /**
   * Keccak-256 hash function.
   *
   * @param input binary encoded input data
   * @return hash value
   */
  public static byte[] sha3(byte[] input) {
    return sha3(input, 0, input.length);
  }

  /**
   * Keccak-256 hash function that operates on a UTF-8 encoded String.
   *
   * @param utf8String UTF-8 encoded string
   * @return hash value as hex encoded string
   */
  public static String sha3String(String utf8String) {
    return Numeric.toHexString(sha3(utf8String.getBytes(StandardCharsets.UTF_8)));
  }

  /**
   * Generates SHA-256 digest for the given {@code input}.
   *
   * @param input The input to digest
   * @return The hash value for the given input
   * @throws RuntimeException If we couldn't find any SHA-256 provider
   */
  public static byte[] sha256(byte[] input) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return digest.digest(input);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("Couldn't find a SHA-256 provider", e);
    }
  }

  public static byte[] hmacSha512(byte[] key, byte[] input) {
    HMac hMac = new HMac(new SHA512Digest());
    hMac.init(new KeyParameter(key));
    hMac.update(input, 0, input.length);
    byte[] out = new byte[64];
    hMac.doFinal(out, 0);
    return out;
  }

  public static byte[] sha256hash160(byte[] input) {
    byte[] sha256 = sha256(input);
    RIPEMD160Digest digest = new RIPEMD160Digest();
    digest.update(sha256, 0, sha256.length);
    byte[] out = new byte[20];
    digest.doFinal(out, 0);
    return out;
  }
}