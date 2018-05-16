# Instructions for submitting your driver
Have you written an Android Things driver for a peripheral? If you would like it to be listed on [androidthings.withgoogle.com][maker-site], follow the instructions below:

## Get your code ready
Take a look at the following resources to ready your driver for the community:
*   Follow the [Android Things Driver Style Guide](https://github.com/androidthings/contrib-drivers/blob/master/driver-style-guide.md) for code style and architectural patterns
*   Browse the [official drivers repository](https://github.com/androidthings/contrib-drivers) to see more examples of Android Things coding conventions
*   Post questions to [StackOverflow](https://stackoverflow.com/questions/tagged/android-things) and [G+](https://g.co/iotdev)

## Host the source code
Host the driver source code on your own GitHub repository. You do not need to add your driver to the [official drivers repository](https://github.com/androidthings/contrib-drivers) to get it listed.

Your GitHub repository can contain one or many drivers.

## Publish your driver binaries
Drivers can be easily added to other projects when they are published as artifacts, compiled and ready to use. We support two ways of publishing driver binaries: through a Maven repository or through a .jar file:

### Publish to a Maven repository (like JCenter)
This is how we publish the official drivers, so feel free to copy the same configuration we have in [contrib-drivers](https://github.com/androidthings/contrib-drivers). Follow the instructions for [distributing Android libraries](https://blog.bintray.com/2017/09/04/the-abcs-of-distributing-android-libraries/) to set your Bintray account and, optionally, link your Bintray artifact to JCenter.

### Publish a .jar file
For simplicity, you can publish your binaries as .jar files. We recommend using [GitHub Releases](https://blog.github.com/2013-07-02-release-your-software/).

## Create a metadata file
Once your driver source code and binaries are published, commit a metadata file to the root directory of your driver source code repository. If your repository contains multiple drivers, each driver has to be in a separate directory with its own metadata file. The metadata file must be at the same level as the driver's build.gradle file and must have the following name and format:

**.android-things-driver.json**
```json
{
  "title": "...",
  "category": "...",
  "samples": ["github_url_of_sample", "github_url_of_another_sample"],
  "published-jar": {
    "location": "github_release_url"
  },
  "published-maven": {
    "maven-url": "url",
    "groupid": "com.example.mycompany",
    "artifactid": "mydriver"
  },
  "compatible-with": [
      {
        "title": "SparkFun Block for Intel Edison",
        "photo": "https://cdn.sparkfun.com/assets/parts/1/1/3/2/1/13770-00.jpg",
        "url": "https://www.sparkfun.com/products/13770",
        "notes": "Only supports a subset of this peripheral"
      },
      {
        "title": "Adafruit ADS1015 12-bit ADC",
        "photo": "https://cdn-shop.adafruit.com/1200x900/1083-00.jpg",
        "url": "https://www.adafruit.com/product/1083"
      }
    ]
  }
```

Notes:
- `title` and `category` are the only mandatory fields
- `published-jar` and `published-maven` are mutually exclusive
- `compatible-with` is a list of actual hardware that this driver is known to work with

## Submit your driver
Once you've followed the steps above, indicate that you want your driver listed on [androidthings.withgoogle.com][maker-site] by filling out the [submission form](https://androidthings.withgoogle.com/submit-a-driver/).

## Remove your driver
If you want to remove your driver from being listed on [androidthings.withgoogle.com][maker-site], just remove (or rename) the .android-things-driver.json metadata file and it will be automatically removed in the following few days.

[maker-site]: https://androidthings.withgoogle.com
