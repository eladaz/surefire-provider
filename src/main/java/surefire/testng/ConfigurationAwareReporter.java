package surefire.testng;

import org.apache.maven.surefire.report.RunListener;
import surefire.testng.TestSuite;
import org.testng.internal.IResultListener;

public class ConfigurationAwareReporter extends Reporter implements IResultListener {
    public ConfigurationAwareReporter(RunListener reportManager, TestSuite source) {
        super(reportManager);
    }
}
