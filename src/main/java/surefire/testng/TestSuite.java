package surefire.testng;

import java.util.Map;

import org.apache.maven.surefire.report.ReporterException;
import org.apache.maven.surefire.report.ReporterFactory;
import org.apache.maven.surefire.testset.TestSetFailedException;

public interface TestSuite {
    void execute(String testSetName, ReporterFactory reporterManagerFactory) throws ReporterException, TestSetFailedException;
    Map locateTestSets(ClassLoader classLoader) throws TestSetFailedException;
}
