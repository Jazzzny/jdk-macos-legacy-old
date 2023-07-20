# Java Development Kit 11 for 10.6 Snow Leopard, 10.7 Lion, 10.8 Mountain Lion, 10.9 Mavericks, 10.10 Yosemite, and 10.11 El Capitan

This is a fork of bleeding-edge upstream OpenJDK 11 with added compatibility for 10.6 Snow Leopard, 10.7 Lion, 10.8 Mountain Lion, 10.9 Mavericks, 10.10 Yosemite, and 10.11 El Capitan.

## Feature Differences from Upstream
- N/A

## Future Goals
- Distribute a JRE variant

## Build Instructions
10.12 Sierra is required to build this JDK.

### Prerequisites
- `autoconf` installed through MacPorts
- Clang 9.0 installed through MacPorts with `+emulated_tls` and `+libstdcxx`
- Java 11 JDK

### Building
1. Clone this repository.
2. `cd` into the repository and run the following configure command:
```bash configure --disable-precompiled-headers --with-debug-level=release --with-native-debug-symbols=none --with-extra-cxxflags="-stdlib=libc++"```
3. Run the following build command: `make images`
4. Wait for the build to complete. This will take a while depending on your hardware configuration.
5. The built JDK will be located in `build/macosx-x86_64-server-release/images/jdk/Contents/Home/`.

To clean the build environment, run `make dist-clean`.

### Known Issues
#### NSCFDictionary leaking on 10.6
On 10.6, the JDK will leak NSCFDictionary objects. This is for an unknown reason and does not occur on 10.7 and later. This should not impact functionality.