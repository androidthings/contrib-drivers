Android Things driver for VCNL4200 proximity sensor and ambient light sensor
===================================

This driver provides support for VCNL4200, a module that serves as both a high-sensitivity
long distance proximity sensor and an ambient light sensor. The module datasheet
is available at https://www.vishay.com/docs/84430/vcnl4200.pdf

NOTE: these drivers are not production-ready. They are offered as sample
implementations of Android Things user space drivers for common peripherals
as part of the Developer Preview release. There is no guarantee
of correctness, completeness or robustness.

How to use the driver
---------------------

### Gradle dependency

To use the `vcnl4200` driver, simply add the line below to your project's `build.gradle`,
where `<version>` matches the last version of the driver available on [jcenter][jcenter].

```
dependencies {
    compile 'com.google.android.things.contrib:driver-vcnl4200:<version>'
}
```

### Sample usage

[TBD]


License
-------

Copyright 2017 Google Inc.

Licensed to the Apache Software Foundation (ASF) under one or more contributor
license agreements.  See the NOTICE file distributed with this work for
additional information regarding copyright ownership.  The ASF licenses this
file to you under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License.  You may obtain a copy of
the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
License for the specific language governing permissions and limitations under
the License.

[product]: https://www.adafruit.com/product/2348
[jcenter]: https://bintray.com/google/androidthings/contrib-driver-vcnl4200/_latestVersion
