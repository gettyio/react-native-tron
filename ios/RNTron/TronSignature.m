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
size_t const kTronSignatureAddressSize = 21;
size_t const kTronSignaturePublicKeySize = 65;
size_t const kTronSignaturePublicKeyHashSize = 20;
size_t const kTronSignaturePrivateKeyLength = 32;
size_t const kTronSignatureHashLength = 32;
size_t const kTronSignatureDataSignatureLength = 65;
uint8_t const kTronSignaturePrefixByteMainnet = 0x41;
uint8_t const kTronSignaturePrefixByteTestnet = 0xa0;

#pragma mark -
#pragma mark Private Functions
#pragma mark

int hdnode_get_tron_pubkeyhash(const HDNode *node, uint8_t *pubkeyhash)
{
    uint8_t buf[65];
    SHA3_CTX ctx;
    
    /* get uncompressed public key */
    ecdsa_get_public_key65(node->curve->params, node->private_key, buf);
    
    /* compute sha3 of x and y coordinate without 04 prefix */
    
    sha3_256_Init(&ctx);
    sha3_Update(&ctx, buf + 1, 64);
    keccak_Final(&ctx, buf);
    
    /* result are the least significant 160 bits */
    memcpy(pubkeyhash, buf + 12, 20);
    
    return 1;
}

@implementation TronSignature

#pragma mark -
#pragma mark Class Methods
#pragma mark

+ (BOOL) validateAddress: (NSString *) address
                 testnet: (BOOL) testnet
{
    uint8_t prefixByte = testnet ? kTronSignaturePrefixByteTestnet : kTronSignaturePrefixByteMainnet;
    NSData *decodedBase58Data = [address decodedBase58Data];
    uint8_t *decodedBase58Bytes = (uint8_t *)[decodedBase58Data bytes];
    return (decodedBase58Data != nil &&
            decodedBase58Data.length == kTronSignatureAddressSize &&
            decodedBase58Bytes[0] == prefixByte);
}

+ (NSString *) generateNewMnemonics
{
    const char *mnemonics = mnemonic_generate(kTronSignatureMnemonicStrength);
    return [NSString stringWithCString: mnemonics encoding: NSUTF8StringEncoding];
}
    
+ (int) validateMnemonic: (NSString *) mnemonic
{
    const char *c = [mnemonic cStringUsingEncoding:NSUTF8StringEncoding];
    return mnemonic_check(c);
}

+ (id) signatureWithMnemonics: (NSString *) mnemonics
                       secret: (NSString *) secret
                   derivePath: (int) derivePath
                      testnet: (BOOL) testnet
{
    return [[TronSignature alloc] initWithMnemonics: mnemonics
                                             secret: secret
                                         derivePath: derivePath
                                            testnet: testnet];
}

+ (id) signatureWithPrivateKey: (NSString *) privateKey
                       testnet: (BOOL) testnet
{
    return [[TronSignature alloc] initWithPrivateKey: privateKey
                                             testnet: testnet];
}

+ (id) generatedSignatureWithSecret: (NSString *) secret
                         derivePath: (int) derivePath
                            testnet: (BOOL) testnet
{
    return [[TronSignature alloc] initWithMnemonics: [TronSignature generateNewMnemonics]
                                             secret: secret
                                         derivePath: derivePath
                                            testnet: testnet];
}

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
        _derivePath = 0;
        _testnet = NO;
        _valid = NO;
    }
    return self;
}

- (id) initWithMnemonics: (NSString *) mnemonics
                  secret: (NSString *) secret
              derivePath: (int) derivePath
                 testnet: (BOOL) testnet
{
    if (self = [self init])
    {
        _mnemonics = mnemonics;
        _secret = secret;
        _derivePath = derivePath;
        _testnet = testnet;
        
        [self _updateFromMnemonics];
    }
    return self;
}

- (id) initWithPrivateKey: (NSString *) privateKey
                  testnet: (BOOL) testnet
{
    if(self = [self init])
    {
        _privateKey = privateKey;
        _testnet = testnet;
        
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
    uint8_t publicKeyBytes[kTronSignaturePublicKeySize];
    const char *scrt = _secret ? [_secret cStringUsingEncoding: NSUTF8StringEncoding] : "";
    
    //Generate seed from mnemonics and populate keys
    mnemonic_to_seed(words, scrt, seed, NULL);
    hdnode_from_seed(seed, kTronSignatureSeedSize, SECP256K1_NAME, &node);
    hdnode_private_ckd(&node, 0x80000000 | 44);
    hdnode_private_ckd(&node, 0x80000000 | 195);
    hdnode_private_ckd(&node, 0x80000000 | _derivePath);
    hdnode_fill_public_key(&node);
    hdnode_get_tron_pubkeyhash(&node, publicKeyHash);
    
    //Get public address
    uint8_t prefixByte = _testnet ? kTronSignaturePrefixByteTestnet : kTronSignaturePrefixByteMainnet;
    memcpy(addr + 1, publicKeyHash, kTronSignaturePublicKeyHashSize);
    addr[0] = prefixByte;
    NSData *addressData = [NSData dataWithBytes: &addr length: kTronSignaturePublicKeyHashSize + 1];
    _address = [NSString encodedBase58StringWithData: addressData];
    
    //Get private key
    NSData *privateKeyData = [NSData dataWithBytes: &node.private_key length: kTronSignaturePrivateKeyLength];
    _privateKey = [privateKeyData hexStringRepresentationUppercase: YES];
    
    //Get public key
    ecdsa_get_public_key65(node.curve->params, node.private_key, publicKeyBytes);
    NSData *publicKeyData = [NSData dataWithBytes: &publicKeyBytes length: kTronSignaturePublicKeySize];
    _publicKey = [publicKeyData hexStringRepresentationUppercase: YES];
    
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
    uint8_t publicKeyBytes[kTronSignaturePublicKeySize];
    const uint8_t *privateKeyBytes = (uint8_t *)[[NSData dataWithHexString: _privateKey] bytes];
    
    //Populate keys
    hdnode_from_xprv(0, 0, privateKeyBytes, privateKeyBytes, SECP256K1_NAME, &node);
    hdnode_fill_public_key(&node);
    hdnode_get_tron_pubkeyhash(&node, publicKeyHash);
    
    //Get public address
    uint8_t prefixByte = _testnet ? kTronSignaturePrefixByteTestnet : kTronSignaturePrefixByteMainnet;
    memcpy(addr + 1, publicKeyHash, kTronSignaturePublicKeyHashSize);
    addr[0] = prefixByte;
    NSData *addressData = [NSData dataWithBytes: &addr length: kTronSignaturePublicKeyHashSize + 1];
    _address = [NSString encodedBase58StringWithData: addressData];
    
    //Get public key
    ecdsa_get_public_key65(node.curve->params, node.private_key, publicKeyBytes);
    NSData *publicKeyData = [NSData dataWithBytes: &publicKeyBytes length: kTronSignaturePublicKeySize];
    _publicKey = [publicKeyData hexStringRepresentationUppercase: YES];
    
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

- (BOOL) testnet
{ return _testnet; }

- (NSString *) mnemonics
{ return _mnemonics; }

- (NSString *) address
{ return _address; }

- (NSString *) privateKey
{ return _privateKey; }

- (NSString *) publicKey
{ return _publicKey; }

@end
