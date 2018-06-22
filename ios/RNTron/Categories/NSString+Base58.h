//
//  NSString+Base58.h
//  walletron
//
//  Created by Brandon Holland on 2018-05-20.
//  Copyright © 2018 Bit-Tree Technologies, Inc. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface NSString (Base58)
+ (NSString *) encodedBase58StringWithData: (NSData *) data;
- (NSData *) decodedBase58Data;
@end
