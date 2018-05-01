# Android Things Driver Style Guide

## Objective

The purpose of this document is to outline the common code style and architecture patterns we recommend for Android Things
drivers. _Consistency_, _readability_ and _maintainability_ are primary considerations for driver code. Most of the drivers
in the [public driver repository](https://github.com/androidthings/contrib-drivers) follow these patterns and can be used as
examples.

## Code style

Code style should adhere to Google's Android [code style](http://source.android.com/source/code-style.html) guidelines.

### Use integer constants for bitwise values

Peripheral drivers contain a disproportionately large number of constants for configuration values and bitwise flags
that need to be written into device registers. Avoid
[enumerated types](https://developer.android.com/reference/java/lang/Enum.html) and favor integers for these constants to
facilitate bitwise combinations:

```java
public static final int ENABLE_A = 0x20;
public static final int ENABLE_B = 0x40;
public static final int ENABLE_C = 0x80;
...
driver.setFlags(ENABLE_A | ENABLE_C);
```

To enforce compile-time usage of proper constants, use the
[IntDef](https://developer.android.com/reference/android/support/annotation/IntDef.html) (and similar) annotations provided
by the Android support library.

Numeric constants and bit flags should be pre-shifted into their appropriate bit indices where possible to simplify AND, OR,
and XOR operations throughout the code.

### Use hex for address constants

Use the hex literal form for all constant values that represent addresses (bus addresses, register addresses, etc.):

```java
public static final int SLAVE_ADDRESS = 0x3C;
...
public static final int REGISTER_CONFIG = 0x0A;
...
```

Format values as complete bytes (2 characters per byte). Do not truncate to nibbles, even when functionally equivalent. For
example, use `0x0F` instead of `0xF,` and `0x0380` instead of `0x380`.

### Use binary for field and flag constants

Use the binary literal form for all constant values that represent flags, modes, or other register data. Apply the minimum
number of digits necessary to describe the entire field, and include a shift when the field is located somewhere other than
the least significant bits:

```java
// Bit 3
public static final int FLAG_INTERRUPT = 1 << 3; //0b00001000
...
// Bits 5 -> 3
public static final int MODE_ACTIVE  = 0b101 << 3; //0b00101000
public static final int MODE_STANDBY = 0b010 << 3; //0b00010000;
...
```

Note that for single-bit flags it is preferred to omit the binary literal prefix.


## Architecture/Design

### Define a peripheral class

For each supported peripheral device, provide a class that wraps the basic
[Peripheral I/O](https://developer.android.com/things/sdk/pio/) operations (reading pin state, reading and writing data
registers, initializing a default configuration, etc.) and exposes higher-level functions and developer-friendly data types
instead.

This class should be named according to the connected peripheral module or integrated circuit (IC), and implement the
`AutoCloseable` interface for proper try-with-resources support:

```java
public class Peripheral implements AutoCloseable {
    ...

    public void setSampleRate(int rate) { ... }

    public float[] readSample() { ... }

    @Override
    public void close() { ... }
}
```

Example: [Bmx280.java](https://github.com/androidthings/contrib-drivers/blob/master/bmx280/src/main/java/com/google/android/things/contrib/driver/bmx280/Bmx280.java)

### Expose features over registers

The peripheral class should expose as many of the chip features as possible, but without leaking the individual registers
necessary to interact with that feature. If multiple registers are involved in configuring a feature or reading a specific
value, the driver should encapsulate all of this logic.

```java
public class Peripheral implements AutoCloseable {
    private static final int REGISTER_VALUEH = 0x01;
    private static final int REGISTER_VALUEL = 0x02;
    private static final int REGISTER_CONFIG = 0x0A;

    public static final int MODE_ACTIVE  = 0b101 << 3; //0b00101000
    public static final int MODE_STANDBY = 0b010 << 3; //0b00010000;

    private I2cDevice mDevice;
    ...

    public void setStandbyMode(boolean standby) {
        byte value = mDevice.readRegByte(REGISTER_CONFIG);
        if (standby) {
            value |= MODE_STANDBY;
        } else {
            value |= MODE_ACTIVE;
        }
        mDevice.writeRegByte(REGISTER_CONFIG, value);
    }

    public int getSensorValue() {
        int result = (mDevice.readRegByte(REGISTER_VALUEH) << 8);
        result += (mDevice.readRegByte(REGISTER_VALUEL) & 0xFF);

        return result;
    }
}
```

### Peripherals are single-use objects

The peripheral class should initialize all Peripheral I/O interfaces in the constructor, and tear those interfaces down in
`close()`. The peripheral class should not contain additional public methods to separately re-initialize the connections.

Each peripheral class should contain a public constructor that constructs peripheral resources from a `Context` and the
resource names.

```java
public class Peripheral implements AutoCloseable {
    private I2cDevice mDevice;
    ...

    // Create a new PIO resource connection
    public Peripheral(Context context, String name, ...) {
        // Initialize PIO
        mDevice = ...;
    }

    @Override
    public void close() {
        // Tear down PIO
        mDevice.close();
    }
}
```

### Always close the underlying PIO interface

PIO devices (`GPIO`, `I2CDevice`, `SPIDevice`, `UartDevice`, etc.) are single-user resources that require explicit closing.
When you open a PIO device in the driver constructor, protect against exceptions that would otherwise return from your
constructor without closing the device properly. For example:

```java
public Bmx280(String bus) throws IOException {
    PeripheralManagerService pioService = new PeripheralManagerService();
    I2cDevice device = pioService.openI2cDevice(bus, I2C_ADDRESS);
    try {
        connect(device);
    } catch (IOException|RuntimeException e) {
        try {
            close();
        } catch (IOException|RuntimeException ignored) {
        }
        throw e;
    }
}
```

### Provide a separate UserDriver class

For peripheral types supported by the Android framework, provide a driver that utilizes the
`com.google.android.things.userdriver` [driver API](https://developer.android.com/things/sdk/drivers/) to bind the peripheral
to the framework. The driver should:

*   Implement `AutoCloseable` for proper try-with-resources support.
*   Configure the peripheral in the constructor with suitable defaults for the associated UserDriver type.
*   Expose any optional configuration for the peripheral as either constructor parameters or setter methods.
*   Provide `register` and `unregister` methods to manage the connection to the framework services.

Driver classes should be named for their UserDriver counterpart:

*   Input drivers → `<Peripheral>InputDriver`
*   Sensor drivers → `<Peripheral>SensorDriver`
    *   Optionally use the more-specific sensor type: `<Peripheral>AccelerometerDriver`
*   GPS drivers → `<Peripheral>GnssDriver`

Each driver class provides a public constructor that creates new peripheral resources from a pin or bus name.

```java
public class ButtonInputDriver implements AutoCloseable {
    private Button mPeripheral;
    ...

    public ButtonInputDriver(String i2cBusName, ...) {
        mPeripheral = new Peripheral(context, i2cBusName, ...);
        ...
    }

    public void setDebounceDelay(int delay) {
        mPeripheral.setDebounceDelay(delay);
    }

    public void register() {
        // Create new user driver
        // Register with UserDriverManager
    }

    public void unregister() {
        // Unegister with UserDriverManager
        // Release user driver
    }

    @Override
    public void close() {
        unregister();
        mPeripheral.close();
    }
}
```

Example: [Bmx280SensorDriver](https://github.com/androidthings/contrib-drivers/blob/master/bmx280/src/main/java/com/google/android/things/contrib/driver/bmx280/Bmx280SensorDriver.java),
[ButtonInputDriver](https://github.com/androidthings/contrib-drivers/blob/master/button/src/main/java/com/google/android/things/contrib/driver/button/ButtonInputDriver.java)

### Meta-drivers

You can create a meta-driver that combines peripherals into more high-level interfaces, for convenience or for
inter-dependency. For example, the
[Rainbow HAT driver](https://github.com/androidthings/contrib-drivers/blob/master/rainbowhat/src/main/java/com/google/android/things/contrib/driver/rainbowhat/RainbowHat.java)
combines several other drivers into a single interface. 

## Testability

### Provide injectable constructors

Include at least one package-private constructor in each public class that allows for the injection of Android framework
dependencies with mocks during unit tests.

For clarity to reviewers, apply the `@VisibleForTesting` annotation.

```java
public class Peripheral {
    @VisibleForTesting
    /*package*/ Peripheral(I2cDevice device, ...) {
        ...
    }
}

public class PeripheralDriver {
    @VisibleForTesting
    /*package*/ PeripheralDriver(UserDriverManager manager,
                                 Peripheral peripheral, ...) {
        ...
    }
}
```

### Mock PIO classes

Tests shouldn't rely on hardware interaction, but most drivers directly (or indirectly) require a PIO class. You can mock
this dependency with frameworks like Mockito.

```java
PioDevice mockPio = Mockito.mock(PioDevice.class);
// may be using an @VisibleForTesting constructor
Driver driver = new Driver(mockPio);
// interact with driver
driver.doSomething(args);
// check interactions with mock
Mockito.verify(mockPio).invocationToVerify(argsToVerify);
```

Recommendations:

*   Use `MockitoRule` to simplify mock creation
*   Use `ExpectedException` to test for exception cases

### Testing event injection

Make a barebones activity in `androidTest` to use for instrumented tests. This activity sets up the peripheral input driver
and registers it with the framework. You will again have to mock the PIO dependency; additionally, you may need an
`@VisibleForTesting`-annotated method to trigger events.

Your test should run on a thread different than the one in which events are received. Typically, events from the framework
are delivered on the main thread, so do not use `@UiThreadTest` for the test. The test triggers a state change in the driver
and waits for the event to arrive using a `LinkedBlockingQueue` (you may need to create an `@VisibleForTesting`-annotated
method to mock state changes that trigger events). Wherever the event is received, add it to the queue so that the test
method can poll the queue and verify the event.

The instrumented test for `ButtonInputDriver` uses `performButtonEvent()` to mock a state change in the driver. Each
`KeyEvent` delivered to the testing activity in `onKeyDown()` or `onKeyUp()` is added to a blocking queue, so the test can
poll for an event and verify that it has the correct key code.

[ButtonInputDriverInstrumentedTest](https://github.com/androidthings/contrib-drivers/blob/master/button/src/androidTest/java/com/google/android/things/contrib/driver/button/ButtonInputDriverInstrumentedTest.java)

[ButtonTestActivity](https://github.com/androidthings/contrib-drivers/blob/master/button/src/androidTest/java/com/google/android/things/contrib/driver/button/ButtonTestActivity.java)

The instrumented test for `Cap12xxInputDriver` stubs interrupts in the driver itself, causing input events to be sent.
Similar to the `ButtonInputDriver` test, these events are added to blocking queues in `onKeyDown()` or `onKeyUp()` and
verified once received.

[Cap12xxInputDriverInstrumentedTest](https://github.com/androidthings/contrib-drivers/blob/master/cap12xx/src/androidTest/java/com/google/android/things/contrib/driver/cap12xx/Cap12xxInputDriverInstrumentedTest.java)

[Cap12xxTestActivity](https://github.com/androidthings/contrib-drivers/blob/master/cap12xx/src/androidTest/java/com/google/android/things/contrib/driver/cap12xx/Cap12xxTestActivity.java)  

Your specific needs may differ slightly from the above examples, but in general you would still use classes from the
`java.util.concurrent` package to handle the asynchronicity of event delivery by the framework. For instance, if you only
need to verify that an event arrived, you could use a `CountDownLatch` and count down wherever the event is received.
Whichever structure you use, prefer using methods that accept a timeout so that the test cannot wait indefinitely.
