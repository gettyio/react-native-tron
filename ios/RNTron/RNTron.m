#import "RNTron.h"

#import <GRPCClient/GRPCCall+Tests.h>
#import <RxLibrary/GRXWriter+Immediate.h>
#import <RxLibrary/GRXWriter+Transformations.h>

#import <NSData+FastHex/NSData+FastHex.h>

#import "Categories/NSString+Base58.h"
#import "TronSignature.h"

@implementation RNTron

RCT_EXPORT_MODULE();

#pragma mark -
#pragma mark Creation + Destruction
#pragma mark

- (id) init
{
    if (self = [super init])
    {
    }
    return self;
}

#pragma mark -
#pragma mark Super Overrides
#pragma mark

- (dispatch_queue_t)methodQueue
{ return dispatch_get_main_queue(); }

+ (BOOL) requiresMainQueueSetup
{ return NO; }

#pragma mark -
#pragma mark Public Native Methods
#pragma mark

RCT_REMAP_METHOD(generateKeypair,
                 mnemonics:(NSString *)mnemonics
                 vaultNumber:(NSNumber * _Nonnull)vaultNumber
                 testnet:(NSNumber * _Nonnull)testnet
                 generateAccountWithResolver:(RCTPromiseResolveBlock)resolve
                 rejecter:(RCTPromiseRejectBlock)reject)
{
    @try
    {
        //Create tron signature for mnemonics and vault number, then verify it is valid
        TronSignature *tronSignature = [TronSignature signatureWithMnemonics: mnemonics
                                                                      secret: nil
                                                                  derivePath: [vaultNumber intValue]
                                                                     testnet: [testnet boolValue]];
        if(!tronSignature.valid)
        {
            //Signature is invalid, reject and return
            reject(@"Failed to restore account from mnemonics", @"Mnemonics invalid", nil);
            return;
        }
        
        //Get password using base64 encoded private key data
        NSData *privateKeyData = [NSData dataWithHexString: tronSignature.privateKey];
        NSString *password = [privateKeyData base64EncodedStringWithOptions: NSDataBase64Encoding64CharacterLineLength];
        
        //Create generated account dictionary
        NSDictionary *returnGeneratedAccount =
        @{
            @"address": tronSignature.address,
            @"privateKey": tronSignature.privateKey,
            @"publicKey": tronSignature.publicKey,
            @"password": password
        };
        
        //Return the restored account dictionary
        resolve(returnGeneratedAccount);
    }
    @catch(NSException *e)
    {
        //Exception, reject
        NSDictionary *userInfo = @{ @"name": e.name, @"reason": e.reason };
        NSError *error = [NSError errorWithDomain: @"io.getty.rntron" code: 0 userInfo: userInfo];
        reject(@"Failed to generate keypair from mnemonics", @"Native exception thrown", error);
    }
}

RCT_REMAP_METHOD(signTransaction,
                 ownerPrivateKey: (NSString *) ownerPrivateKey
                 encodedTransaction:(NSString *)encodedTransaction
                 testnet:(NSNumber * _Nonnull)testnet
                 signTransactionWithResolver:(RCTPromiseResolveBlock)resolve
                 rejecter:(RCTPromiseRejectBlock)reject)
{
    @try
    {
        //Create tron signature for private key, then verify it is valid
        TronSignature *tronSignature = [TronSignature signatureWithPrivateKey: ownerPrivateKey
                                                                      testnet: [testnet boolValue]];
        if(!tronSignature.valid)
        {
            //Signature is invalid, reject and return
            reject(@"Failed to sign transaction", @"Owner private key invalid", nil);
            return;
        }
        
        //Decode base64 encoded string to data
        NSData *transactionData = [[NSData alloc] initWithBase64EncodedString: encodedTransaction options: NSDataBase64DecodingIgnoreUnknownCharacters];
        
        //Attempt to parse transaction from data
        NSError *transactionParseError = nil;
        Transaction *transaction = [Transaction parseFromData: transactionData error: &transactionParseError];
        if(!transaction || transactionParseError)
        {
            //Problem decoding/parsing transaction, reject and return
            reject(@"Failed to sign transaction", @"Decoder/parser error", nil);
            return;
        }
        
        //Set transaction timestamp and get signature
        transaction.rawData.timestamp = ([NSDate timeIntervalSinceReferenceDate] * 1000);
        NSData *signatureData = [tronSignature sign: transaction.rawData.data];
        
        //Add signature for each contract in transaction (Each contract could have a different signature in the future)
        for(int i = 0; i < transaction.rawData.contractArray_Count; i++)
        { [transaction.signatureArray addObject: signatureData]; }
        
        //Get base64 encoded string of signed transaction
        NSString *base64EncodedTransaction = [transaction.data base64EncodedStringWithOptions: NSDataBase64Encoding64CharacterLineLength];
        
        //Return result
        resolve(base64EncodedTransaction);
    }
    @catch(NSException *e)
    {
        //Exception, reject
        NSDictionary *userInfo = @{ @"name": e.name, @"reason": e.reason };
        NSError *error = [NSError errorWithDomain: @"io.getty.rntron" code: 0 userInfo: userInfo];
        reject(@"Failed to sign transaction", @"Native exception thrown", error);
    }
}

@end
