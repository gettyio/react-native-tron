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
    NSString *_secret;
    BOOL _valid;
    BOOL _fromWords;
}
+ (BOOL) validatePublicKey: (NSString *) publicKey;
+ (NSString *) generateNewMnemonics;
+ (id) signatureWithMnemonics: (NSString *) mnemonics
                       secret: (NSString *) secret;
+ (id) signatureWithPrivateKey: (NSString *) privateKey;
+ (id) generatedSignatureWithSecret: (NSString *) secret;
- (id) initWithMnemonics: (NSString *) mnemonics
                  secret: (NSString *) secret;
- (id) initWithPrivateKey: (NSString *) privateKey;
- (NSData *) sign: (NSData *) data;
- (BOOL) valid;
- (BOOL) fromWords;
- (NSString *) mnemonics;
- (NSString *) address;
- (NSString *) privateKey;
@end
