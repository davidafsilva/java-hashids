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

### Instantiation with Salt
```java
final Hashids hashids = Hashids.newInstance("my awesome salt");
final String encoded = hashids.encode(1,2,3,4,5);
final long[] decoded = hashids.decode(encoded);
```

### Instantiation with Salt and Alphabet
```java
final Hashids hashids = Hashids.newInstance("my awesome salt", "0123456789abcdef");
final String encoded = hashids.encode(1,2,3,4,5);
final long[] decoded = hashids.decode(encoded);
```

### Instantiation with Salt, custom Alphabet and minimum hash length
```java
final Hashids hashids = Hashids.newInstance("my awesome salt", "0123456789abcdef", 32);
final String encoded = hashids.encode(1,2,3,4,5);
final long[] decoded = hashids.decode(encoded);
```
