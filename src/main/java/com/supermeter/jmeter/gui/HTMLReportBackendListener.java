package com.supermeter.jmeter.gui;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.visualizers.backend.AbstractBackendListenerClient;
import org.apache.jmeter.visualizers.backend.BackendListenerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple HTML Report Backend Listener for JMeter.
 * Generates beautiful HTML reports with real-time statistics.
 */
public class HTMLReportBackendListener extends AbstractBackendListenerClient {

    private static final Logger log = LoggerFactory.getLogger(HTMLReportBackendListener.class);
    private static final String DEFAULT_REPORT_FILE = "report.html";
    private static final String DEFAULT_MAX_SAMPLES = "1000";
    private static final String DEFAULT_INCLUDE_SUCCESSFUL = "true";
    private static final int REPORT_UPDATE_INTERVAL_SECONDS = 5;

    private String reportFilePath;
    private boolean includeSuccessfulSamples;
    private int maxSampleDetails;

    private final AtomicInteger totalSamples = new AtomicInteger(0);
    private final AtomicInteger failedSamples = new AtomicInteger(0);
    private final AtomicLong totalResponseTime = new AtomicLong(0);
    private final AtomicLong minResponseTime = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong maxResponseTime = new AtomicLong(0);
    private final ConcurrentLinkedQueue<SampleResult> sampleResults = new ConcurrentLinkedQueue<>();
    private long testStartTime;

    private ScheduledExecutorService scheduler;

    @Override
    public Arguments getDefaultParameters() {
        Arguments arguments = new Arguments();
        arguments.addArgument("reportFilePath", "${__P(reportPath," + DEFAULT_REPORT_FILE + ")}");
        arguments.addArgument("includeSuccessfulSamples", DEFAULT_INCLUDE_SUCCESSFUL);
        arguments.addArgument("maxSampleDetails", DEFAULT_MAX_SAMPLES);
        return arguments;
    }

    @Override
    public void setupTest(BackendListenerContext context) throws Exception {
        super.setupTest(context);
        reportFilePath = context.getParameter("reportFilePath", DEFAULT_REPORT_FILE);
        includeSuccessfulSamples = context.getBooleanParameter("includeSuccessfulSamples", Boolean.parseBoolean(DEFAULT_INCLUDE_SUCCESSFUL));
        maxSampleDetails = context.getIntParameter("maxSampleDetails", Integer.parseInt(DEFAULT_MAX_SAMPLES));
        testStartTime = System.currentTimeMillis();

        File reportFile = new File(reportFilePath);
        if (!reportFile.getParentFile().exists()) {
            reportFile.getParentFile().mkdirs();
        }

        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::generateRealTimeReport, REPORT_UPDATE_INTERVAL_SECONDS, REPORT_UPDATE_INTERVAL_SECONDS, TimeUnit.SECONDS);

        log.info("HTML Report Backend Listener initialized. Report will be saved to: {}", reportFilePath);
    }

    @Override
    public void handleSampleResults(List<SampleResult> results, BackendListenerContext context) {
        for (SampleResult result : results) {
            totalSamples.incrementAndGet();
            long responseTime = result.getTime();
            totalResponseTime.addAndGet(responseTime);

            minResponseTime.updateAndGet(current -> Math.min(current, responseTime));
            maxResponseTime.updateAndGet(current -> Math.max(current, responseTime));

            if (!result.isSuccessful()) {
                failedSamples.incrementAndGet();
            }

            if (includeSuccessfulSamples || !result.isSuccessful()) {
                if (sampleResults.size() >= maxSampleDetails) {
                    sampleResults.poll(); // Remove oldest element to maintain size
                }
                sampleResults.add(result);
            }
        }
    }

    @Override
    public void teardownTest(BackendListenerContext context) throws Exception {
        super.teardownTest(context);
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(REPORT_UPDATE_INTERVAL_SECONDS, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        generateCompleteReport();
        log.info("HTML Report completed: {}", reportFilePath);
    }

    private void generateRealTimeReport() {
        try {
            generateReport("JMeter Performance Test Report (Real-time)");
        } catch (IOException e) {
            log.error("Failed to generate real-time HTML report", e);
        }
    }

    private void generateCompleteReport() {
        try {
            generateReport("JMeter Performance Test Report (Final)");
        } catch (IOException e) {
            log.error("Failed to generate final HTML report", e);
        }
    }

    private synchronized void generateReport(String title) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(reportFilePath))) {
            int total = totalSamples.get();
            int failed = failedSamples.get();
            int successful = total - failed;
            double successRate = total > 0 ? (successful * 100.0 / total) : 0;
            double avgResponseTime = total > 0 ? (totalResponseTime.get() / (double) total) : 0;
            long testDuration = System.currentTimeMillis() - testStartTime;
            double throughput = total > 0 ? (total / (testDuration / 1000.0)) : 0;

            writer.println("<!DOCTYPE html>");
            writer.println("<html lang='en'>");
            writer.println("<head>");
            writer.println("    <meta charset='UTF-8'>");
            writer.println("    <meta name='viewport' content='width=device-width, initial-scale=1.0'>");
            writer.println("    <title>" + title + "</title>");
            writer.println("    <style>");
            writer.println("        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; margin: 20px; background-color: #f0f2f5; color: #333; }");
            writer.println("        .container { max-width: 1200px; margin: 0 auto; background: #fff; padding: 25px; border-radius: 10px; box-shadow: 0 4px 12px rgba(0,0,0,0.1); }");
            writer.println("        h1 { color: #1a1a1a; text-align: center; margin-bottom: 30px; font-weight: 600; }");
            writer.println("        .stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 20px; margin-bottom: 30px; }");
            writer.println("        .stat-card { background: #f8f9fa; padding: 20px; border-radius: 8px; border-left: 5px solid #007bff; text-align: center; }");
            writer.println("        .stat-card.error { border-left-color: #dc3545; }");
            writer.println("        .stat-card.success { border-left-color: #28a745; }");
            writer.println("        .stat-title { font-size: 14px; color: #555; text-transform: uppercase; margin-bottom: 10px; font-weight: 500; }");
            writer.println("        .stat-value { font-size: 28px; font-weight: 700; color: #000; }");
            writer.println("        .samples-table { width: 100%; border-collapse: collapse; margin-top: 30px; box-shadow: 0 2px 4px rgba(0,0,0,0.05); }");
            writer.println("        .samples-table th, .samples-table td { padding: 12px 15px; text-align: left; border-bottom: 1px solid #e9ecef; }");
            writer.println("        .samples-table th { background-color: #f8f9fa; font-weight: 600; }");
            writer.println("        .samples-table tr:hover { background-color: #f1f3f5; }");
            writer.println("        .status-success { color: #28a745; font-weight: bold; }");
            writer.println("        .status-failed { color: #dc3545; font-weight: bold; }");
            writer.println("        .updated-time { text-align: center; color: #666; font-size: 12px; margin-top: 25px; }");
            writer.println("        .progress-container { margin-bottom: 30px; }");
            writer.println("        .progress-bar { width: 100%; height: 24px; background-color: #e9ecef; border-radius: 12px; overflow: hidden; }");
            writer.println("        .progress-fill { height: 100%; transition: width 0.4s ease-in-out; text-align: center; color: white; font-weight: bold; line-height: 24px; }");
            writer.println("        .progress-success { background-color: #28a745; }");
            writer.println("    </style>");
            writer.println("</head>");
            writer.println("<body>");
            writer.println("    <div class='container'>");
            writer.println("        <h1>" + title + "</h1>");
            
            // Statistics section
            writer.println("        <div class='stats-grid'>");
            addStatCard(writer, "Total Samples", String.valueOf(total), "");
            addStatCard(writer, "Successful", String.valueOf(successful), "success");
            addStatCard(writer, "Failed", String.valueOf(failed), "error");
            addStatCard(writer, "Success Rate", String.format("%.2f%%", successRate), "success");
            addStatCard(writer, "Avg Response Time", String.format("%.0f ms", avgResponseTime), "");
            addStatCard(writer, "Min Response Time", (minResponseTime.get() == Long.MAX_VALUE ? "0" : minResponseTime.get()) + " ms", "");
            addStatCard(writer, "Max Response Time", maxResponseTime.get() + " ms", "");
            addStatCard(writer, "Throughput", String.format("%.2f/sec", throughput), "");
            writer.println("        </div>");

            // Success rate progress bar
            writer.println("        <div class='progress-container'>");
            writer.println("            <div class='progress-bar'>");
            writer.println("                <div class='progress-fill progress-success' style='width: " + successRate + "%;'>" + String.format("%.1f%%", successRate) + "</div>");
            writer.println("            </div>");
            writer.println("        </div>");

            // Sample details table
            if (!sampleResults.isEmpty()) {
                writer.println("        <h3>Sample Details (Last " + sampleResults.size() + " samples)</h3>");
                writer.println("        <table class='samples-table'>");
                writer.println("            <thead><tr><th>Time</th><th>Sample</th><th>Status</th><th>Response Time (ms)</th><th>Response Code</th><th>Thread</th></tr></thead>");
                writer.println("            <tbody>");

                SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
                for (SampleResult sample : sampleResults) {
                    String statusClass = sample.isSuccessful() ? "status-success" : "status-failed";
                    String status = sample.isSuccessful() ? "SUCCESS" : "FAILED";
                    writer.println("                <tr>");
                    writer.println("                    <td>" + dateFormat.format(new Date(sample.getTimeStamp())) + "</td>");
                    writer.println("                    <td>" + escapeHtml(sample.getSampleLabel()) + "</td>");
                    writer.println("                    <td class='" + statusClass + "'>" + status + "</td>");
                    writer.println("                    <td>" + sample.getTime() + "</td>");
                    writer.println("                    <td>" + sample.getResponseCode() + "</td>");
                    writer.println("                    <td>" + escapeHtml(sample.getThreadName()) + "</td>");
                    writer.println("                </tr>");
                }
                writer.println("            </tbody>");
                writer.println("        </table>");
            }

            writer.println("        <div class='updated-time'>Last updated: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "</div>");
            writer.println("    </div>");
            writer.println("</body>");
            writer.println("</html>");
        }
    }

    private void addStatCard(PrintWriter writer, String title, String value, String type) {
        writer.println("            <div class='stat-card " + type + "'>");
        writer.println("                <div class='stat-title'>" + title + "</div>");
        writer.println("                <div class='stat-value'>" + value + "</div>");
        writer.println("            </div>");
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#x27;");
    }
}
 