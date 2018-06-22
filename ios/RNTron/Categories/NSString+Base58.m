//
//  NSString+Base58.m
//  walletron
//
//  Created by Brandon Holland on 2018-05-20.
//  Copyright © 2018 Bit-Tree Technologies, Inc. All rights reserved.
//

#import "NSString+Base58.h"
#import <TrezorCrypto/sha2.h>
#import <TrezorCrypto/base58.h>

size_t const kBase58Sha256Length = 32;
size_t const kBase58EncodedLength = 36;
size_t const kBase58DecodedLength = 25;
size_t const kBase58HashLength = 4;
uint8_t const kBase58PrefixByte = 0x41; //0xa0 for testnet

@implementation NSString (Base58)

+ (NSString *) encodedBase58StringWithData: (NSData *) data
{
    //Hash the data, then hash the hash
    SHA256_CTX ctx;
    uint8_t hash0[kBase58Sha256Length];
    uint8_t hash1[kBase58Sha256Length];
    sha256_Init(&ctx);
    sha256_Update(&ctx, data.bytes, data.length);
    sha256_Final(&ctx, hash0);
    sha256_End(&ctx, NULL);
    sha256_Init(&ctx);
    sha256_Update(&ctx, hash0, kBase58Sha256Length);
    sha256_Final(&ctx, hash1);
    sha256_End(&ctx, NULL);
    
    //Append the hash to the end of the data
    uint8_t addr_data[kBase58DecodedLength];
    memcpy(addr_data, data.bytes, data.length);
    memcpy(addr_data + (kBase58DecodedLength - kBase58HashLength), hash1, kBase58HashLength);
    
    //Create buffer to store base58 encoded string
    char addr[kBase58EncodedLength];
    size_t addr_size = kBase58EncodedLength;
    
    //Attempt to encode the base58 string
    int result = b58enc(addr, &addr_size, addr_data, kBase58DecodedLength);
    if(!result)
    { return nil; }

    //Return the base58 encoded string
    return [NSString stringWithCString: addr encoding: NSUTF8StringEncoding];
}

- (NSData *) decodedBase58Data
{
    //Get utf8 bytes of the string (and length)
    const char *address = [self UTF8String];
    
    //Create buffer to store decoded data
    uint8_t addressBytes[kBase58DecodedLength];
    size_t addressSize = kBase58DecodedLength;
    
    //Attempt to decode the base58 encoded string
    bool result = b58tobin(addressBytes, &addressSize, address);
    if(!result)
    { return nil; }
    
    //Verify the base58 data
    int check = b58check(addressBytes, addressSize, HASHER_SHA2, address);
    if(check != kBase58PrefixByte)
    { return nil; }

    //Return the decoded base58 data
    return [NSData dataWithBytes: addressBytes length: (kBase58DecodedLength - kBase58HashLength)];
}

@end
