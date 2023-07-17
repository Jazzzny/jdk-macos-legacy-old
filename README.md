# Java Development Kit 17 for 10.8 Mountain Lion, 10.9 Mavericks, 10.10 Yosemite, and 10.11 El Capitan

This is a fork of OpenJDK 17 (specifically the 17-0-ga tag) with added compatibility for OS X 10.8 Mountain Lion, OS X 10.9 Mavericks, OS X 10.10 Yosemite, and OS X 10.11 El Capitan.

## Feature Differences from Upstream
- Metal graphics support has been removed

## Future Goals
- ~~Embed the MacPorts `legacy-support` dynamic library~~ ✅
- Distribute a JRE variant
- Backport security patches (Ongoing)
- ~~10.8 Mountain Lion support~~ ✅
- Improve 10.8 support
- Invesigate feasibility of 10.7 Lion support

## Build Instructions
10.12 Sierra is required to build this JDK.

### Prerequisites
- `autoconf` installed through MacPorts
- Java 16 JDK

### Building
1. Clone this repository.
2. `cd` into the repository and run the following configure command:
```bash configure --disable-precompiled-headers --with-debug-level=release --with-native-debug-symbols=none --with-extra-cxxflags="-stdlib=libc++"```
3. Run the following build command: `make images`
4. Wait for the build to complete. This will take a while depending on your hardware configuration.
5. The built JDK will be located in `build/macosx-x86_64-server-release/images/jdk/Contents/Home/`.
6. Use install_name_tool to change libjvm.dylib's reliance on libSystem.B.dylib to the legacy-support library.
To clean the build environment, run `make dist-clean`.

### Known Issues
#### 10.8 Mountain Lion build is botched
Building the 10.8 variant of the JDK requires changing the minimum target from 10.9 to 10.8 in various files. This is not the default as the build fails to perform build optimizations, resulting in a very slow JDK. Additionally, the JDK image fails to build, meaning that you must manually piece together the JDK.