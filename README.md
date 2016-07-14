Brillo user-space drivers
=========================

Reusable drivers for Brillo.

Pre-requisites
--------------

- com.google.brillo:brillo-sdk


Build and install
=================

1. If necessary, increment the packageVersion for each driver in `<driver>/build.gradle`
2. Run `./gradlew publish`

If all works, each driver will be compiled, packaged and published to a Maven
repository.

For consistency, it is recommended that all drivers in this directory are
published under the groupdId of "com.google.brillo.driver". For example, the
`servo` driver version 0.3 is published as `com.google.brillo.driver:servo:0.3`


How to use it
=============

To use a driver published from this repository in another project, in addition
to the Brillo SDK, you just need the corresponding driver dependency
in your build.gradle:

```
dependencies {
    compile 'com.google.brillo:brillo-sdk:0.2'
    compile 'com.google.brillo.driver:servo:0.3'
}
```

(changing the versions if necessary)

Until Brillo is launched, the "Pre-requisites" section from
go/brillo-gradle-setup also needs to be fulfilled, since Gradle needs to
know how to find the private Maven repo.


License
-------

Copyright 2016 The Android Open Source Project, Inc.

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
