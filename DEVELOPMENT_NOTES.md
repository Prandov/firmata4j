# Development Notes

## Baseline

- Preserve the working SGP30 example before the shared I2C sensor overhaul.
- Baseline commit: `4d1ff0e` (`Replace jSSC with jSerialComm and update SLF4J`).
- Local checkpoint tag: `checkpoint/sgp30-before-i2c-overhaul`.
- SGP30 communication was previously confirmed on hardware. The checkpoint has not been retested on hardware.
- The existing experimental `I2CSensor` package is included in the baseline; review it before deciding what to reuse.
- The development build now excludes `org/firmata4j/I2CSensor/**` through the Maven compiler configuration. Remove this temporary exclusion when the replacement framework is ready.
- The checkpoint Maven configuration targets Java 7 and fails on Java 8 lambdas. Commit `0d71619` established the Java 8 baseline; the development build now targets Java 21.

## Planned Work

### Java 21

- [x] Set the project compiler release to Java 21.
- [x] Update the compiler and test plugins, CI Java configuration, and build documentation for Java 21.
- [x] Select JDK 21 for the local IntelliJ project SDK, Maven runner, and importer, then reload Maven. IDE settings are not tracked in Git.
- [x] Verify the project builds and tests successfully with JDK 21.

Validation on 2026-11-16: `mvn clean verify` passed using portable Corretto
21.0.13, with all six tests passing in the normal forked JVM. No
`-DforkCount=0` workaround is required with Surefire 3.5.4. The JDK was
downloaded into the system temporary directory for validation; install or
select a persistent JDK 21 for everyday development. GitHub workflows were
updated but have not been run remotely. The user subsequently installed
Corretto 21.0.13 permanently and confirmed SGP30 initialization and repeated
readings, plus BMP280 chip identification (`0x58`), on Java 21.

Remaining build follow-up: replace the deprecated `Double(double)` constructor
in `ParsingI2CMessageState` during parser cleanup.

### Logging

- [x] Use SLF4J consistently in active library and sensor-example code. The excluded experimental package remains for the I2C overhaul.
- [x] Replace active diagnostic console output with appropriate logging levels: ERROR, WARN, INFO, DEBUG, and TRACE.
- [x] Keep routine connection and sensor lifecycle messages at INFO; put command details and raw payloads at DEBUG or TRACE.
- [x] Document logging backend configuration for applications and examples, while keeping the library dependent on the SLF4J API.

The `example-logging` Maven profile supplies optional runtime `slf4j-simple`
2.0.20 for local examples. Normal builds have no runtime logging backend or
bundled application logging configuration. Tests default to INFO. The README
documents Maven profile activation, IntelliJ VM options, package-specific log
levels, timestamps, and optional file output. Sensor readings appear at INFO
in examples; the SGP30 library logs readings at DEBUG to avoid duplicate output.
Serial/I2C hex dumps use TRACE, with lazy formatting on the transport path.

Validation on 2026-11-17: ten unit tests passed with and without the example
profile. The actual BMP280 example on COM5 passed checks for INFO output,
TRACE payload output, and WARN suppression, with no missing-provider warning.
Maven quiet mode affects logging when running examples inside Maven's JVM;
use the documented package-level properties to control application detail.
One hardware regression run caught a CRC mismatch in the SGP30 feature-set
response (received CRC 0x64, calculated 0x65). This remains an intermittent
communication observation; no retries were added to hide the failed check.
A diagnostic rerun with I2C TRACE enabled passed all three hardware tests,
including 16 SGP30 readings and interleaved BMP280 ID reads. That successful
rerun does not establish the cause or resolution of the earlier CRC mismatch.

### Dependencies

- [x] Review and update all dependencies to their latest appropriate stable releases when this work is performed.
- [x] Review and update Maven build plugins as well as runtime and test dependencies.
- [x] Check Java 21 compatibility, migration requirements, and relevant release notes.
- [x] Run tests and verify serial communication after updates.

Dependency review on 2026-11-17 used Maven Central release metadata and
upstream migration documentation. Pre-release versions were excluded.

- jSerialComm remains at 2.11.4, the latest stable version.
- SLF4J API and the test provider move together to 2.0.20. Applications need
  a compatible 2.x provider; examples can use the `example-logging` profile.
- JUnit moves to 4.13.2, the latest 4.x release. Migration to Jupiter is a
  separate test-framework change; existing tests remain on JUnit 4.
- Compiler 3.16.0 and Surefire 3.6.1 replace the Java 21 baseline versions.
  Surefire now runs the JUnit 4 suite through the JUnit Platform provider.
- Clean 3.5.0, resources 3.5.0, jar 3.5.1, install 3.2.0, and deploy 3.2.0
  are pinned explicitly rather than inherited from old Maven defaults.
- Release 3.3.1, exec 3.7.0, source 3.4.0, Javadoc 3.12.0, and GPG 3.2.8
  replace the legacy tooling. Exec raises the required Maven version to 3.9.0.
- `mvn clean verify` passes all six tests on Corretto 21.0.13 and Maven 3.9.16.
  Hardware was subsequently validated by the opt-in integration tests below.
- `mvn -Prelease -Dgpg.skip=true verify` also passes, producing source and
  Javadoc jars without signing or deploying artifacts.
- Maven 3.9.16 is installed permanently at
  `C:\Users\epran\.maven\apache-maven-3.9.16`. User `MAVEN_HOME`, `PATH`,
  and `JAVA_HOME` (Corretto 21.0.13) are configured. Restart terminals or the
  IDE to pick up changed environment variables.

Publishing configuration still references the legacy OSSRH service and must
be reviewed separately before publishing. Release validation must skip GPG
signing unless signing credentials are configured; validation never deploys.

### I2C Test Coverage

- Four hardware-independent regression tests protect register-less callback
  delivery, explicit-register priority, one-shot callback removal, and separate
  pending reads for different I2C addresses. The unit suite now has ten tests.
- Opt-in Failsafe tests use `mvn -Phardware-tests -Dfirmata.port=COM5 verify`.
  Add `-Dfirmata.sgp30=true` when the external SGP30 is connected.
- Hardware baseline: Grove Beginner Kit configured as Arduino Nano, COM5,
  StandardFirmata 2.5.7, protocol source version 2.5.1. The protocol response
  exposes only 2.5, and the firmware report also exposes major/minor only.
- Validation on 2026-11-17: ten unit tests and three hardware tests passed on
  Corretto 21.0.13. The hardware checks covered initialization, protocol 2.5,
  BMP280 ID 0x58 at address 0x77, and 16 CRC-valid SGP30 readings at 0x58
  with interleaved BMP280 ID reads. The serial connection was closed afterward.
- These raw I2C hardware checks do not validate BMP280 pressure conversion,
  the SGP30 wrapper API, or long-term measurement accuracy. Unit coverage for
  malformed sensor payloads and CRC rejection remains future work.

### Common I2C Sensor Package

The protocol 2.5 audit and implementation changes are documented in
`docs/PROTOCOL_2_5.md`, with line comments in the affected Java code. This adds
version-gated single-pin digital writes and opt-in repeated-start reads, while
correcting I2C delay/register encoding and reserved pin-mode handling. Optional
Firmata extension APIs are outside this update. Existing sensor APIs and the
experimental package exclusion are retained.

Validation on 2026-11-17: 23 unit tests and all three COM5 hardware tests pass,
including BMP280 registered repeated-start reads and 16 CRC-valid SGP30 samples.
The board's 2.5 protocol report no longer produces a version mismatch warning.
Tests deliberately simulating protocol 2.3 and 2.4 still produce compatibility
warnings to exercise the older-firmware fallback. Single-pin output writes are
verified with a fake transport rather than driving the Grove kit's outputs.

- [ ] Expand I2C device support and move sensor implementations into a common package.
- [ ] Decide package naming and structure after reviewing the existing sensor code and experimental `I2CSensor` package.
- [ ] Evaluate a shared interface, abstract base class, factory, or helper classes where they reduce duplication.
- [ ] Keep sensor commands, timing requirements, and payload decoding in their respective sensor classes.
- [ ] Design event-driven reads with typed readings, error reporting, and optional cached latest values.
- [ ] Schedule sensor execution delays without blocking the Firmata callback thread.
- [ ] Define request serialization, reply matching, timeouts, and resource cleanup for multiple sensors.
- [ ] Keep device address, word length, and future bus-selection metadata in mind; verify transport support before exposing multiple-bus selection.

Provide or complete classes and runnable examples for:

- [ ] BMP280.
- [ ] DHT20.
- [ ] SGP30.
- [ ] LIS3DH.

## Suggested Order

1. Establish a Java 21 build and test baseline.
2. Update dependencies and build plugins, validating changes in manageable steps.
3. Implement consistent SLF4J logging and document configuration.
4. Design the shared I2C package around the working SGP30 command/response flow.
5. Implement and validate each sensor and its example, then test several sensors together.

## I2C Findings To Preserve

- Register-less requests use `REGISTER_NOT_SET (-1)` internally in firmata4j, while the tested firmware returns register `0x00` in replies.
- The callback fallback in `FirmataI2CDevice.onReceive(...)` restored delivery of SGP30 replies. Preserve this behavior and test reply matching when revisiting the transport code.
- Sensor execution time and the timeout for receiving a reply through Firmata are separate concerns.
- The current SGP30 helper wraps asynchronous callbacks in a blocking polling loop. An event-driven replacement was discussed but has not been implemented.
- Request callbacks are currently keyed by register within each device. Multiple requests to the same sensor need explicit coordination.

These notes describe planned work; unchecked items are not implemented or verified.
