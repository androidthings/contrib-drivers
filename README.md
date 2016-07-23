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
    compile 'com.google.brillo:brillo-sdk:0.3'
    compile 'com.google.brillo.driver:servo:0.3'
}
```

(changing the versions if necessary)

Until Brillo is launched, the "Pre-requisites" section from
go/brillo-gradle-setup also needs to be fulfilled, since Gradle needs to
know how to find the private Maven repo.


How to publish a new driver
===========================

In order to set your Gradle configuration to publish your driver with a single command, you need
to configure your ~/.gradle/init.gradle as described in go/brillo-gradle-setup and add
this snippet to your project's build.gradle:

```
apply plugin: 'maven-publish'

def packageVersion = '0.1'

task sourceJar(type: Jar) {
    classifier = 'sources'
    from android.sourceSets.main.java.sourceFiles
}

publishing {
    assert project.hasProperty('BRILLO_REPOSITORY'): \
       "Property BRILLO_REPOSITORY not set.\n" +
            "Please, set your ~/.gradle/init.gradle as instructed in go/brillo-gradle-setup"
    publications {
        mavenJava(MavenPublication) {
            groupId 'com.google.brillo.driver'
            artifactId '<YOUR_ARTIFACT_ID>'
            version packageVersion
            artifacts = configurations.archives.artifacts
            artifact sourceJar
        }
    }
    repositories {
        maven {
            url BRILLO_REPOSITORY
            credentials(AwsCredentials) {
                accessKey AWS_ACCESS_KEY
                secretKey AWS_SECRET_KEY
            }
        }
    }
}
```

Then, change the packageVersion appropriately and run `./gradlew YOURPROJECT:publish`

For example, `./gradlew servo:publish`

NOTE: You can use the existing build.gradle in `servo` as an example.


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
