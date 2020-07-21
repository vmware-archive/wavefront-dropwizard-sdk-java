package com.wavefront.sdk.dropwizard.reporter;

import com.codahale.metrics.MetricRegistry;
import com.wavefront.dropwizard.metrics.DropwizardMetricsReporter;
import com.wavefront.sdk.common.WavefrontSender;
import com.wavefront.sdk.common.application.ApplicationTags;
import com.wavefront.sdk.common.application.HeartbeaterService;
import com.wavefront.sdk.common.metrics.WavefrontSdkMetricsRegistry;
import com.wavefront.sdk.entities.metrics.WavefrontMetricSender;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import static com.wavefront.sdk.common.Constants.APPLICATION_TAG_KEY;
import static com.wavefront.sdk.common.Constants.CLUSTER_TAG_KEY;
import static com.wavefront.sdk.common.Constants.NULL_TAG_VAL;
import static com.wavefront.sdk.common.Constants.SDK_METRIC_PREFIX;
import static com.wavefront.sdk.common.Constants.SERVICE_TAG_KEY;
import static com.wavefront.sdk.common.Constants.SHARD_TAG_KEY;
import static com.wavefront.sdk.common.Utils.getSemVer;
import static com.wavefront.sdk.dropwizard.Constants.DROPWIZARD_COMPONENT;

/**
 * Wavefront Dropwizard SDK reporter that collects metrics and histograms from your Dropwizard
 * application and reports to Wavefront.
 *
 * @author Sushant Dewan (sushant@wavefront.com).
 */
public class WavefrontDropwizardReporter {

  private final DropwizardMetricsReporter dropwizardMetricsReporter;
  private final int reportingIntervalSeconds;
  private final HeartbeaterService heartbeaterService;

  private WavefrontDropwizardReporter(DropwizardMetricsReporter dropwizardMetricsReporter,
                                      int reportingIntervalSeconds,
                                      WavefrontMetricSender wavefrontMetricSender,
                                      ApplicationTags applicationTags,
                                      String source) {
    this.dropwizardMetricsReporter = dropwizardMetricsReporter;
    this.reportingIntervalSeconds = reportingIntervalSeconds;
    heartbeaterService = new HeartbeaterService(wavefrontMetricSender, applicationTags,
        Collections.singletonList(DROPWIZARD_COMPONENT), source);
  }

  /**
   * Start the Dropwizard reporter so that it can periodically report data about your Dropwizard
   * application to Wavefront.
   */
  public void start() {
    dropwizardMetricsReporter.start(reportingIntervalSeconds, TimeUnit.SECONDS);
  }

  /**
   * Stop the reporter. Invoke this method before your JVM shuts down.
   */
  public void stop() {
    dropwizardMetricsReporter.stop();
    heartbeaterService.close();
  }

  public static class Builder {
    // Required parameters
    private final MetricRegistry metricRegistry;
    private final ApplicationTags applicationTags;
    private final String prefix = "dw";

    // Optional parameters
    private int reportingIntervalSeconds = 60;

    @Nullable
    private String source;

    /**
     * Builder to build WavefrontDropwizardReporter.
     * @param metricRegistry    dropwizard application environment metric registry
     * @param applicationTags   metadata about your application that you want to be propagated as
     *                          tags when metrics/histograms are sent to Wavefront.
     */
    public Builder(MetricRegistry metricRegistry, ApplicationTags applicationTags) {
      this.metricRegistry = metricRegistry;
      this.applicationTags = applicationTags;
    }

    /**
     * Set reporting interval i.e. how often you want to report the metrics/histograms to
     * Wavefront.
     *
     * @param reportingIntervalSeconds reporting interval in seconds.
     * @return {@code this}.
     */
    public Builder reportingIntervalSeconds(int reportingIntervalSeconds) {
      this.reportingIntervalSeconds = reportingIntervalSeconds;
      return this;
    }

    /**
     * Set the source tag for your metric and histograms.
     *
     * @param source Name of the source/host where your application is running.
     * @return {@code this}.
     */
    public Builder withSource(String source) {
      this.source = source;
      return this;
    }

    /**
     * Build WavefrontDropwizardReporter.
     *
     * @param wavefrontSender send data to Wavefront via proxy or direct ingestion.
     * @return An instance of {@link WavefrontDropwizardReporter}.
     */
    public WavefrontDropwizardReporter build(WavefrontSender wavefrontSender) {
      if (source == null) {
        try {
          source = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
          // Should never happen
          source = "unknown";
        }
      }

      Map<String, String> pointTags = new HashMap<>();
      pointTags.put(APPLICATION_TAG_KEY, applicationTags.getApplication());
      pointTags.put(SERVICE_TAG_KEY, applicationTags.getService());
      pointTags.put(CLUSTER_TAG_KEY,
          applicationTags.getCluster() == null ? NULL_TAG_VAL : applicationTags.getCluster());
      pointTags.put(SHARD_TAG_KEY,
          applicationTags.getShard() == null ? NULL_TAG_VAL : applicationTags.getShard());
      if (applicationTags.getCustomTags() != null) {
        pointTags.putAll(applicationTags.getCustomTags());
      }

      DropwizardMetricsReporter dropwizardMetricsReporter = DropwizardMetricsReporter.forRegistry
          (metricRegistry).prefixedWith(prefix).withSource(source).withReporterPointTags
          (pointTags).build(wavefrontSender);

      WavefrontSdkMetricsRegistry sdkMetricsRegistry = new WavefrontSdkMetricsRegistry.
          Builder(wavefrontSender).prefix(SDK_METRIC_PREFIX + ".wavefront_dropwizard.reporter").
          source(source).tags(pointTags).build();

      double sdkVersion = getSemVer();
      sdkMetricsRegistry.newGauge("version", () -> sdkVersion);

      return new WavefrontDropwizardReporter(dropwizardMetricsReporter, reportingIntervalSeconds,
          wavefrontSender, applicationTags, source);
    }
  }
}
