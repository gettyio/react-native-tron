//
//  TronSignature.h
//  RNTron
//
//  Created by Brandon Holland on 2018-05-22.
//  Copyright © 2018 GettyIO. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface TronSignature : NSObject
{
    NSString *_mnemonics;
    NSString *_address;
    NSString *_privateKey;
    NSString *_publicKey;
    NSString *_secret;
    int _derivePath;
    BOOL _testnet;
    BOOL _valid;
}
+ (BOOL) validateAddress: (NSString *) address
                 testnet: (BOOL) testnet;
    
+ (int) validateMnemonic: (NSString *) mnemonic;
    
+ (NSString *) generateNewMnemonics;
+ (id) signatureWithMnemonics: (NSString *) mnemonics
                       secret: (NSString *) secret
                   derivePath: (int) derivePath
                      testnet: (BOOL) testnet;
+ (id) signatureWithPrivateKey: (NSString *) privateKey
                       testnet: (BOOL) testnet;
+ (id) generatedSignatureWithSecret: (NSString *) secret
                         derivePath: (int) derivePath
                            testnet: (BOOL) testnet;
- (id) initWithMnemonics: (NSString *) mnemonics
                  secret: (NSString *) secret
              derivePath: (int) derivePath
                 testnet: (BOOL) testnet;
- (id) initWithPrivateKey: (NSString *) privateKey
                  testnet: (BOOL) testnet;
- (NSData *) sign: (NSData *) data;
- (BOOL) valid;
- (BOOL) testnet;
- (NSString *) mnemonics;
- (NSString *) address;
- (NSString *) privateKey;
- (NSString *) publicKey;
@end
