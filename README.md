```
VMware has ended active development of this project, this repository will no longer be updated.
```
# Wavefront Dropwizard SDK 

The Wavefront by VMware Dropwizard SDK provides out of the box metrics for your Dropwizard application. You can analyze the data in [Wavefront](https://www.wavefront.com) to better understand how your application is performing in production.

## Maven
If you are using Maven, add the following maven dependency to your pom.xml:
```xml
<dependency>
    <groupId>com.wavefront</groupId>
    <artifactId>wavefront-dropwizard-sdk-java</artifactId>
    <version>$releaseVersion</version>
</dependency>
```

Replace `$releaseVersion` with the latest version available on [maven](http://search.maven.org/#search%7Cga%7C1%7Cwavefront-dropwizard-sdk-java).

## Set Up a WavefrontDropwizardReporter
This SDK provides a `WavefrontDropwizardReporter` for collecting metrics about your Dropwizard application.

To create a `WavefrontDropwizardReporter`:
1. Create an `ApplicationTags` instance, which specifies metadata metadata about your application.
2. Create a `WavefrontSender` for sending data to Wavefront.
3. Create a `WavefrontDropwizardReporter` instance.

For the details of each step, see the sections below.

### 1. Set Up Application Tags

The application tags determine the metadata (point tags) that are included with the Dropwizard metrics reported to Wavefront. These tags enable you to filter and query the reported Dropwizard metrics in Wavefront.

You encapsulate application tags in an `ApplicationTags` object.
See [Instantiating ApplicationTags](https://github.com/wavefrontHQ/wavefront-sdk-doc-sources/blob/master/java/applicationtags.md#application-tags) for details.

### 2. Set Up a WavefrontSender

A `WavefrontSender` object implements the low-level interface for sending data to Wavefront. You can choose to send data using either the [Wavefront proxy](https://docs.wavefront.com/proxies.html) or [direct ingestion](https://docs.wavefront.com/direct_ingestion.html).

* If you have already set up a `WavefrontSender` for another SDK that will run in the same JVM, use that one.  (For details about sharing a `WavefrontSender` instance, see [Share a WavefrontSender](https://github.com/wavefrontHQ/wavefront-sdk-doc-sources/blob/master/java/wavefrontsender.md#Share-a-WavefrontSender).)

* Otherwise, follow the steps in [Set Up a WavefrontSender](https://github.com/wavefrontHQ/wavefront-sdk-doc-sources/blob/master/java/wavefrontsender.md#wavefrontsender).


### 3. Create the WavefrontDropwizardReporter
A `WavefrontDropwizardReporter` reports metrics to Wavefront.

To build a `WavefrontDropwizardReporter`, you must specify:
* Dropwizard Environment `MetricRegistry` object.
* An `ApplicationTags` object.
* A `WavefrontSender` object.

You can optionally specify:
* A nondefault source for the reported data. If you omit the source, the host name is automatically used. The source should be identical across all the Wavefront SDKs running in the same JVM.
* A nondefault reporting interval, which controls how often data is reported to the `WavefrontSender`. The reporting interval determines the timestamps on the data sent to Wavefront. If you omit the reporting interval, data is reported once a minute.

```java

ApplicationTags applicationTags = buildTags(); // pseudocode; see above
WavefrontSender wavefrontSender = buildWavefrontSender(); // pseudocode; see above

/**
 * Every Dropwizard application has an `io.dropwizard.setup.Environment` instance.
 * Grab the metric registry for your Dropwizard environment i.e. `io.dropwizard.setup.Environment.metrics()`
 */
MetricRegistry metricRegistry = environment.metrics();

// Create WavefrontDropwizardReporter.Builder using applicationTags
WavefrontDropwizardReporter.Builder wfDropwizardReporterBuilder = new WavefrontDropwizardReporter.Builder(metricRegistry, applicationTags);

// Optionally set the source name to "mySource" for your metrics and histograms.
// Omit this statement to use the host name.
wfDropwizardReporterBuilder.withSource("mySource");

// Optionally change the reporting interval to 30 seconds. Default is 1 minute.
wfDropwizardReporterBuilder.reportingIntervalSeconds(30);

// Create a WavefrontDropwizardReporter with the WavefronSender.
WavefrontDropwizardReporter wfDropwizardReporter = wfDropwizardReporterBuilder.build(wavefrontSender);
```

## Start the WavefrontDropwizardReporter
You start the `WavefrontDropwizardReporter` explicitly to start reporting Dropwizard app metrics.

```java
// Start the reporter
wfDropwizardReporter.start();
```

## Stop the WavefrontDropwizardReporter
You must explicitly stop the `WavefrontDropwizardReporter` before shutting down your JVM.

```java
// Stop the reporter
wfDropwizardReporter.stop();
```

## Dropwizard app metrics

You can go to Wavefront and see the Dropwizard app metrics with the prefix `dw.*`.
