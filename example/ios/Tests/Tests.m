//
//  Tests.m
//  Tests
//
//  Created by Dio Ianakiara on 2019-05-06.
//  Copyright © 2019 Facebook. All rights reserved.
//

#import <XCTest/XCTest.h>
#import <RNTron/TronSignature.h>

@interface Tests : XCTestCase

@end

@implementation Tests

- (void)setUp {
    // Put setup code here. This method is called before the invocation of each test method in the class.
}

- (void)tearDown {
    // Put teardown code here. This method is called after the invocation of each test method in the class.
}

- (void)testExample {
  @try
  {
    //Create tron signature for mnemonics and vault number, then verify it is valid
    TronSignature *tronSignature = [TronSignature signatureWithMnemonics: @"hello"
                                                                  secret: nil
                                                              derivePath: [@"1" intValue]
                                                                 testnet: 1];
//      if(!tronSignature.valid)
//      {
//        //Signature is invalid, reject and return
//        reject(@"Failed to restore account from mnemonics", @"Mnemonics invalid", nil);
//        return;
//      }
//
//      //Get password using base64 encoded private key data
//      NSData *privateKeyData = [NSData dataWithHexString: tronSignature.privateKey];
//      NSString *password = [privateKeyData base64EncodedStringWithOptions: NSDataBase64Encoding64CharacterLineLength];
//      
//      //Create generated account dictionary
//      NSDictionary *returnGeneratedAccount =
//      @{
//        @"address": tronSignature.address,
//        @"privateKey": tronSignature.privateKey,
//        @"publicKey": tronSignature.publicKey,
//        @"password": password
//        };
//
//      //Return the restored account dictionary
//      resolve(returnGeneratedAccount);
  }
  @catch(NSException *e)
  {
    //Exception, reject
    NSDictionary *userInfo = @{ @"name": e.name, @"reason": e.reason };
    NSError *error = [NSError errorWithDomain: @"io.getty.rntron" code: 0 userInfo: userInfo];
//    reject(@"Failed to generate keypair from mnemonics", @"Native exception thrown", error);
  }
}

- (void)testPerformanceExample {
    // This is an example of a performance test case.
    [self measureBlock:^{
        // Put the code you want to measure the time of here.
    }];
}

@end
