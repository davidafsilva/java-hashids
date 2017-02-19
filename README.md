# java-hashids
Implementation of the Hashids hashing protocol.

The underlying implementation shall be, in most cases, compatible/interchangeable with the
[standard implementation](https://github.com/ivanakimov/hashids.js) provided by the author of the 
protocol.
Compatibility with other implementations is expected as long as they cope with the standard 
implementation as well.


## Usage

Instances of the implementation are available throught the usage of the static factory methods, 
which support a variety of parameters.

### Instantiation with a salt
```java
final Hashids hashids = Hashids.newInstance("my awesome salt");
final String encoded = hashids.encode(1,2,3,4,5); // encoded = "lmh8S9cQuk"
final long[] decoded = hashids.decode(encoded);   // decoded = [1, 2, 3, 4, 5]
```

### Instantiation with a salt and a custom alphabet
```java
final Hashids hashids = Hashids.newInstance("my awesome salt", "0123456789abcdef");
final String encoded = hashids.encode(1,2,3,4,5); // encoded = "24c20519fb"
final long[] decoded = hashids.decode(encoded);   // decoded = [1, 2, 3, 4, 5]
```

### Instantiation with a salt, custom alphabet and minimum hash length
```java
final Hashids hashids = Hashids.newInstance("my awesome salt", "0123456789abcdef", 32);
final String encoded = hashids.encode(1,2,3,4,5); // encoded = "a924d54a937624c20519fb6de82b835b"
final long[] decoded = hashids.decode(encoded);   // decoded = [1, 2, 3, 4, 5]
```

### Encode hex representation (vs long)
```java
final Hashids hashids = Hashids.newInstance("my awesome salt");
final String encoded = hashids.encodeHex("507f1f77bcf86cd799439011"); // encoded = "y42LW46J9lhq3Xq9XMly"
final String decoded = hashids.decodeHex(encoded);   // decoded = 507f1f77bcf86cd799439011
```
