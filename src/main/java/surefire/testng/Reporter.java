package surefire.testng;

import java.util.ResourceBundle;
import org.apache.maven.surefire.report.CategorizedReportEntry;
import org.apache.maven.surefire.report.PojoStackTraceWriter;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.RunListener;
import org.apache.maven.surefire.report.SimpleReportEntry;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

public class Reporter implements ITestListener, ISuiteListener {

    private static final String         SUREFIRE_BUNDLE_NAME = "org.apache.maven.surefire.surefire";
    private        final ResourceBundle bundle               = ResourceBundle.getBundle(SUREFIRE_BUNDLE_NAME);
    private        final RunListener    reporter;

    public Reporter(RunListener reportManager) {
        this.reporter = reportManager;
        if (reportManager == null) {
            throw new IllegalArgumentException("ReportManager passed in was null.");
        }
    }

    public void onTestStart(ITestResult result) {
        String group = groupString(result.getMethod().getGroups(), result.getTestClass().getName());
        ReportEntry report = new CategorizedReportEntry(getSource(result), getUserFriendlyTestName(result), group);
        reporter.testStarting(report);
    }

    private String getSource(ITestResult result) {
        return result.getTestClass().getName();
    }

    public void onTestSuccess(ITestResult result) {
        ReportEntry report = new SimpleReportEntry(getSource(result), getUserFriendlyTestName(result));
        reporter.testSucceeded(report);
    }

    public void onTestFailure(ITestResult result) {
        String realClassName = result.getTestClass().getRealClass().getName();
        String methodName    = result.getMethod().getMethodName();
        PojoStackTraceWriter writer = new PojoStackTraceWriter(realClassName, methodName, result.getThrowable());
        ReportEntry report = SimpleReportEntry.withException(getSource(result), getUserFriendlyTestName(result), writer);
        reporter.testFailed(report);
    }

    private static String getUserFriendlyTestName(ITestResult result) {
        return result.getName() + "(" + result.getTestClass().getName() + ")"; // This is consistent with the JUnit output
    }

    public void onTestSkipped(ITestResult result) {
        ReportEntry report = new SimpleReportEntry(getSource(result), getUserFriendlyTestName(result));
        reporter.testSkipped(report);
    }

    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
        String realClassName = result.getTestClass().getRealClass().getName();
        String methodName    = result.getMethod().getMethodName();
        PojoStackTraceWriter writer = new PojoStackTraceWriter(realClassName, methodName, result.getThrowable());
        ReportEntry report = SimpleReportEntry.withException(getSource(result), getUserFriendlyTestName(result), writer);
        reporter.testError(report);
    }

    public void onStart(ITestContext context) { }

    public void onFinish(ITestContext context) { }

    public void onStart(ISuite suite) { }

    public void onFinish(ISuite suite) { }

    private static String groupString(String[] groups, String defaultValue) {
        String retVal;
        if (groups != null && groups.length > 0) {
            StringBuilder str = new StringBuilder();
            for (int i = 0; i < groups.length; i++) {
                str.append(groups[i]);
                if (i + 1 < groups.length) {
                    str.append(",");
                }
            }
            retVal = str.toString();
        } else {
            retVal = defaultValue;
        }
        return retVal;
    }

    public void onConfigurationFailure(ITestResult result) {
        onTestFailure(result);
    }

    public void onConfigurationSkip(ITestResult result) {
        onTestSkipped(result);
    }

    public void onConfigurationSuccess(ITestResult result) {
        // DGF Don't record configuration successes as separate tests
        //onTestSuccess( result );
    }

}
