# java-hashids [![Build Status](https://travis-ci.org/davidafsilva/java-hashids.svg?branch=master)](https://travis-ci.org/davidafsilva/java-hashids) [![Coverage Status](https://img.shields.io/coveralls/davidafsilva/java-hashids/master.svg)](https://coveralls.io/github/davidafsilva/java-hashids?branch=master)
Implementation of the [Hashids](http://hashids.org) hashing protocol, which defines how to generate short, unique, non-sequential identifiers (hashes) from numbers.

The underlying implementation is compatible/interchangeable with the [JavaScript implementation](https://github.com/ivanakimov/hashids.js) provided by the author of the protocol.
Compatibility with other implementations is expected as long as they cope with the JavaScript implementation as well.

## Import

Add the jCenter repository and include the following coordinates:
Maven:
```xml
<dependency>
  <groupId>pt.davidafsilva.hashids</groupId>
  <artifactId>java-hashids</artifactId>
  <version>1.0.0</version>
  <type>pom</type>
</dependency>
```
Gradle:
```
compile 'pt.davidafsilva.hashids:java-hashids:1.0.0'
```

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
final String encoded = hashids.encodeHex("507f1f77bcf86cd799439011"); // encoded = "R2qnd2vkOJTXm7XV7yq4"
final String decoded = hashids.decodeHex(encoded);   // decoded = "507f1f77bcf86cd799439011"
```
