//
//  TronSignature.m
//  RNTron
//
//  Created by Brandon Holland on 2018-05-22.
//  Copyright © 2018 GettyIO. All rights reserved.
//

#import "TronSignature.h"

#import <TrezorCrypto/TrezorCrypto.h>
#import <NSData+FastHex/NSData+FastHex.h>
#import "Categories/NSString+Base58.h"
#include "memzero.h"

size_t const kTronSignatureMnemonicStrength = 128;
size_t const kTronSignatureSeedSize = 64;
size_t const kTronSignaturePublicKeySize = 21;
size_t const kTronSignaturePublicKeyHashSize = 20;
size_t const kTronSignaturePrivateKeyLength = 32;
size_t const kTronSignatureHashLength = 32;
size_t const kTronSignatureDataSignatureLength = 65;
uint8_t const kTronSignaturePrefixByte = 0x41; //0xa0 for testnet

#pragma mark -
#pragma mark Private Functions
#pragma mark

const curve_info secp256k1_tron_info = {
    .bip32_name = "Tron seed",
    .params = &secp256k1,
    .hasher_type = HASHER_SHA2,
};

int hdnode_from_seed_tron(const uint8_t *seed, int seed_len, HDNode *out)
{
    static CONFIDENTIAL uint8_t I[32 + 32];
    memset(out, 0, sizeof(HDNode));
    out->depth = 0;
    out->child_num = 0;
    out->curve = &secp256k1_tron_info;
    
    static CONFIDENTIAL HMAC_SHA512_CTX ctx;
    hmac_sha512_Init(&ctx, (const uint8_t*) out->curve->bip32_name, strlen(out->curve->bip32_name));
    hmac_sha512_Update(&ctx, seed, seed_len);
    hmac_sha512_Final(&ctx, I);
    
    memcpy(out->private_key, I, 32);
    memcpy(out->chain_code, I + 32, 32);
    memzero(out->public_key, sizeof(out->public_key));
    memzero(I, sizeof(I));
    return 1;
}

@implementation TronSignature

#pragma mark -
#pragma mark Class Methods
#pragma mark

+ (BOOL) validatePublicKey: (NSString *) publicKey
{
    NSData *decodedBase58Data = [publicKey decodedBase58Data];
    uint8_t *decodedBase58Bytes = (uint8_t *)[decodedBase58Data bytes];
    return (decodedBase58Data != nil &&
            decodedBase58Data.length == kTronSignaturePublicKeySize &&
            decodedBase58Bytes[0] == kTronSignaturePrefixByte);
}

+ (NSString *) generateNewMnemonics
{
    const char *mnemonics = mnemonic_generate(kTronSignatureMnemonicStrength);
    return [NSString stringWithCString: mnemonics encoding: NSUTF8StringEncoding];
}

+ (id) signatureWithMnemonics: (NSString *) mnemonics
                       secret: (NSString *) secret
{ return [[TronSignature alloc] initWithMnemonics: mnemonics secret: secret]; }

+ (id) signatureWithPrivateKey: (NSString *) privateKey
{ return [[TronSignature alloc] initWithPrivateKey: privateKey]; }

+ (id) generatedSignatureWithSecret: (NSString *) secret
{ return [[TronSignature alloc] initWithMnemonics: [TronSignature generateNewMnemonics] secret: secret]; }

#pragma mark -
#pragma mark Creation + Destruction
#pragma mark

- (id) init
{
    if (self = [super init])
    {
        _mnemonics = nil;
        _address = nil;
        _privateKey = nil;
        _secret = nil;
        _valid = NO;
        _fromWords = NO;
    }
    return self;
}

- (id) initWithMnemonics: (NSString *) mnemonics
                  secret: (NSString *) secret
{
    if (self = [self init])
    {
        _mnemonics = mnemonics;
        _secret = secret;
        _fromWords = YES;
        
        [self _updateFromMnemonics];
    }
    return self;
}

- (id) initWithPrivateKey: (NSString *) privateKey
{
    if(self = [self init])
    {
        _privateKey = privateKey;
        _fromWords = NO;
        
        [self _updateFromPrivateKey];
    }
    return self;
}

#pragma mark -
#pragma mark Private Methods
#pragma mark

- (void) _updateFromMnemonics
{
    //Verify mnemonics is not null
    if(!_mnemonics)
    {
        //Invalidate signature and return
        _valid = NO;
        return;
    }
    
    //Verify mnemonics are valid
    const char *words = [_mnemonics cStringUsingEncoding: NSUTF8StringEncoding];
    if(!mnemonic_check(words))
    {
        //Invalidate signature and return
        _valid = NO;
        return;
    }
    
    //Declare variables
    HDNode node;
    uint8_t seed[kTronSignatureSeedSize];
    uint8_t publicKeyHash[kTronSignaturePublicKeyHashSize];
    uint8_t addr[kTronSignaturePublicKeyHashSize + 1];
    const char *scrt = _secret ? [_secret cStringUsingEncoding: NSUTF8StringEncoding] : "";
    
    //Generate seed from mnemonics and populate keys
    mnemonic_to_seed(words, scrt, seed, NULL);
    hdnode_from_seed_tron(seed, kTronSignatureSeedSize, &node);
    hdnode_fill_public_key(&node);
    hdnode_get_ethereum_pubkeyhash(&node, publicKeyHash);
    
    //Get public address from seed
    memcpy(addr + 1, publicKeyHash, kTronSignaturePublicKeyHashSize);
    addr[0] = kTronSignaturePrefixByte;
    NSData *addressData = [NSData dataWithBytes: &addr length: kTronSignaturePublicKeyHashSize + 1];
    _address = [NSString encodedBase58StringWithData: addressData];
    
    //Get private key from seed
    NSData *privateKeyData = [NSData dataWithBytes: &node.private_key length: kTronSignaturePrivateKeyLength];
    _privateKey = [privateKeyData hexStringRepresentationUppercase: YES];
    
    //Signature is valid if we made it this far
    _valid = YES;
}

- (void) _updateFromPrivateKey
{
    if(!_privateKey || _privateKey.length != kTronSignaturePrivateKeyLength * 2)
    {
        _valid = NO;
        return;
    }
    
    //Declare variables
    HDNode node;
    uint8_t publicKeyHash[kTronSignaturePublicKeyHashSize];
    uint8_t addr[kTronSignaturePublicKeyHashSize + 1];
    const uint8_t *privateKeyBytes = (uint8_t *)[[NSData dataWithHexString: _privateKey] bytes];
    
    //Populate keys
    hdnode_from_xprv(0, 0, privateKeyBytes, privateKeyBytes, SECP256K1_NAME, &node);
    hdnode_fill_public_key(&node);
    hdnode_get_ethereum_pubkeyhash(&node, publicKeyHash);
    
    //Get public address
    memcpy(addr + 1, publicKeyHash, kTronSignaturePublicKeyHashSize);
    addr[0] = kTronSignaturePrefixByte;
    NSData *addressData = [NSData dataWithBytes: &addr length: kTronSignaturePublicKeyHashSize + 1];
    _address = [NSString encodedBase58StringWithData: addressData];
    
    //Signature is valid if we made it this far
    _valid = YES;
}

#pragma mark -
#pragma mark Public Methods
#pragma mark

- (NSData *) sign: (NSData *) data
{
    //Get sha256 hash of data
    uint8_t hash[kTronSignatureHashLength];
    SHA256_CTX ctx;
    sha256_Init(&ctx);
    sha256_Update(&ctx, [data bytes], [data length]);
    sha256_Final(&ctx, hash);
    sha256_End(&ctx, NULL);
    
    //Declare variables
    HDNode node;
    uint8_t pby;
    uint8_t signatureBytes[kTronSignatureDataSignatureLength];
    const uint8_t *privateKeyBytes = (uint8_t *)[[NSData dataWithHexString: _privateKey] bytes];
    
    //Populate key
    hdnode_from_xprv(0, 0, privateKeyBytes, privateKeyBytes, SECP256K1_NAME, &node);
    
    //Attempt to sign the data
    int result = hdnode_sign_digest(&node, hash, signatureBytes, &pby, NULL);
    if(result != 0)
    { return nil; }
    
    //Set version
    signatureBytes[64] = pby + 27;
    
    //Return data signature
    return [NSData dataWithBytes: signatureBytes length: kTronSignatureDataSignatureLength];
}

#pragma mark -
#pragma mark Accessors
#pragma mark

- (BOOL) valid
{ return _valid; }

- (BOOL) fromWords
{ return _fromWords; }

- (NSString *) mnemonics
{ return _mnemonics; }

- (NSString *) address
{ return _address; }

- (NSString *) privateKey
{ return _privateKey; }

@end


