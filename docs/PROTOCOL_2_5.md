# Firmata 2.5 Compatibility

This fork targets core protocol 2.5 with the 2.5.1 I2C repeated-start feature.
The reference hardware is StandardFirmata 2.5.7 on the Grove Beginner Kit
configured as Arduino Nano. Firmware version and protocol version are separate;
REPORT_VERSION transmits protocol major/minor only, so patch 1 is not detectable
from that response. The Java library artifact version remains independent.

## Audit And Changes

| Feature | Status |
| --- | --- |
| Firmware, protocol, capability, pin-state, and analog-mapping queries | Existing support retained; protocol version is explicitly requested at startup |
| Initial analog/digital reports on enabling reporting (2.4) | Existing parser supports them; covered by regression fixtures |
| INPUT_PULLUP mode | Existing mode and input event handling retained; mode 0x0B encoding tested |
| Single digital pin write (2.5, 0xF5) | Used by output pins after the board reports protocol 2.5+; legacy port writes remain for older/unknown versions |
| I2C repeated start (2.5.1, flag 0x40) | Opt-in for one-shot and continuous registered reads |
| I2C delay configuration | Corrected two-7-bit encoding, including delay 128; supports 0..16383 microseconds |
| I2C reply register | Preserves unsigned 14-bit header field instead of narrowing to a signed byte |
| Malformed I2C reply | Reports a parser error and recovers for the next message |
| Pin-mode encoding | IGNORE uses 0x7F; reserved mode values resolve to UNSUPPORTED |
| Legacy digital port writes | Corrected unsigned high bit and bounded partial final ports |

Changes are explained by comments at the affected code locations. Core
message fixtures and version-gating tests run without hardware. The hardware
suite checks protocol 2.5, BMP280 ID using both normal and repeated-start reads,
and SGP30 responses with interleaved BMP280 reads.

## Repeated-Start Reads

Use repeated start when the sensor datasheet requires a register read without
releasing the bus after selecting the register. It changes this sequence:

```text
Normal:   START -> address(W) -> register -> STOP
          START -> address(R) -> data -> STOP
Repeated: START -> address(W) -> register -> REPEATED START -> address(R) -> data -> STOP
```

BMP280 ID reads accept both forms; its ID register at 0xD0 is a convenient
example, not a claim that BMP280 requires repeated start.

```java
I2CDevice sensor = board.getI2CDevice((byte) 0x77);
sensor.setReadRestart(true);
sensor.ask(0xD0, (byte) 1, event -> {
    int chipId = event.getData()[0] & 0xFF; // Interpret the reply byte as unsigned.
});
```

The setting applies to subsequent read requests. Change it before starting
continuous updates; stop and restart updates to change an active request.
It controls the boundary between firmware register selection and reading.
It does not combine separate tell() and ask() requests into one transaction.
Default behavior remains STOP. Enable it only with compatible firmware;
the protocol patch version is not available for automatic detection.

Call `sensor.setReadRestart(false)` to restore STOP for future reads. Leave it
disabled if the sensor needs a STOP. In particular, SGP30 uses a command write,
a sensor execution delay, and then a register-less read. This setting does not
remove that execution delay or turn the command into a register address.

## Delay And Response Timeout

```java
sensor.setDelay(128); // 128 microseconds inside firmware, after register selection.
```

The delay is encoded in two seven-bit wire bytes: `low = delay & 0x7F` and
`high = (delay >> 7) & 0x7F`. Thus 128 is `[0, 1]`, and the representable range
is 0..16383 microseconds. The previous implementation incorrectly encoded the
128 boundary and restricted the range to 255.

This delay is board-wide in StandardFirmata. firmata4j keeps the largest value
requested by any device; a later smaller value does not lower it. Set it only
when required by the sensor, because it affects other devices too.

This is distinct from both a sensor command's execution delay (for example,
waiting before requesting SGP30 results) and the host response timeout (how
long Java waits for a callback). `setDelay()` configures neither of those.

## Register And Pin-Mode Encoding

I2C reply registers are assembled into an integer from two seven-bit fields.
The register `0xD0` therefore stays 208; narrowing it to Java's signed byte
would produce -48 and prevent matching the callback for `ask(0xD0, ...)`.
Payload bytes still use Java's `byte[]`; apply `value & 0xFF` when an unsigned
numeric value is needed.

Pin modes also use defined wire values rather than arbitrary enum positions.
`pin.setMode(Pin.Mode.PULLUP)` sends mode 0x0B to enable the input pull-up,
provided the pin reports support. `Pin.Mode.IGNORED` uses 0x7F, not its enum
ordinal. Unknown/reserved mode values resolve to `UNSUPPORTED` instead of
indexing past the enum or being mistaken for another mode. Applications must
check supported modes before requesting a mode change.

## Scope

Optional extensions such as serial forwarding, OneWire, steppers, encoders,
and scheduling do not gain dedicated Java APIs from this update. I2C uses
7-bit slave addresses and one firmware bus; 10-bit addressing and multiple-bus
selection remain unsupported. StandardFirmata selects an 8-bit sensor register
even though the protocol header can carry a wider register field. Single-pin
GPIO writes are unit-tested but the hardware suite does not drive outputs.
An earlier intermittent SGP30 CRC mismatch remains an open observation.

References: [core protocol](https://github.com/firmata/protocol/blob/master/protocol.md),
[I2C protocol](https://github.com/firmata/protocol/blob/master/i2c.md),
[StandardFirmata 2.5.7](https://github.com/firmata/arduino/blob/2.5.7/examples/StandardFirmata/StandardFirmata.ino),
[2.5.7 constants](https://github.com/firmata/arduino/blob/2.5.7/FirmataConstants.h).
The protocol repository evolves beyond 2.5; later features are outside this audit.
