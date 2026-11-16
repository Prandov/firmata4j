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
- [ ] Select JDK 21 for the local IntelliJ project SDK, Maven runner, and importer, then reload Maven. IDE settings are not tracked in Git.
- [x] Verify the project builds and tests successfully with JDK 21.

Validation on 2026-11-16: `mvn clean verify` passed using portable Corretto
21.0.13, with all six tests passing in the normal forked JVM. No
`-DforkCount=0` workaround is required with Surefire 3.5.4. The JDK was
downloaded into the system temporary directory for validation; install or
select a persistent JDK 21 for everyday development. GitHub workflows were
updated but have not been run remotely. Hardware examples have not been
retested on Java 21.

Remaining build follow-ups: review the legacy release-profile plugins during
the dependency update, and replace the deprecated `Double(double)` constructor
in `ParsingI2CMessageState` during parser cleanup.

### Logging

- [ ] Use SLF4J consistently throughout the project.
- [ ] Replace diagnostic console output with appropriate logging levels: ERROR, WARN, INFO, DEBUG, and TRACE.
- [ ] Keep routine connection and sensor lifecycle messages at INFO; put command details and raw payloads at DEBUG or TRACE.
- [ ] Document logging backend configuration for applications and examples, while keeping the library dependent on the SLF4J API.

### Dependencies

- [ ] Review and update all dependencies to their latest appropriate stable releases when this work is performed.
- [ ] Review and update Maven build plugins as well as runtime and test dependencies.
- [ ] Check Java 21 compatibility, migration requirements, and relevant release notes.
- [ ] Run tests and verify serial communication after updates.

### Common I2C Sensor Package

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
