
Pod::Spec.new do |s|
  s.name         = "RNTron"
  s.version      = "1.0.0"
  s.summary      = "React Native Tron Library"
  s.description  = "React Native Tron Library"
  s.homepage     = "https://getty.io"
  s.license      = "MIT"
  s.author       = { "Brandon Holland" => "bholland@brandon-holland.com" }
  s.platform     = :ios, "9.0"
  s.source       = "https://github.com/gettyio/react-native-tron.git"
  s.source_files = "RNTron/**/*.{h,m}"
  s.requires_arc = true

  s.dependency "TrezorCrypto"
  s.dependency "NSData+FastHex"
  s.dependency "gRPC-ProtoRPC"
  s.dependency "Protobuf"
end
