package surefire.testng;

import java.util.*;

import org.apache.maven.surefire.providerapi.AbstractProvider;
import org.apache.maven.surefire.providerapi.ProviderParameters;
import org.apache.maven.surefire.report.ReporterConfiguration;
import org.apache.maven.surefire.report.ReporterException;
import org.apache.maven.surefire.report.ReporterFactory;
import org.apache.maven.surefire.suite.RunResult;
import org.apache.maven.surefire.testset.TestRequest;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.apache.maven.surefire.util.RunOrderCalculator;
import org.apache.maven.surefire.util.ScanResult;
import org.apache.maven.surefire.util.TestsToRun;

public class TiserProvider extends AbstractProvider {

    private final Map<String, String>   providerProperties;
    private final ReporterConfiguration reporterConfiguration;
    private final ClassLoader           testClassLoader;
    private final ScanResult            scanResult;
    private final TestRequest           testRequest;
    private final ProviderParameters    providerParameters;
    private       TestsToRun            testsToRun;
    private final RunOrderCalculator    runOrderCalculator;

    public TiserProvider(ProviderParameters booterParameters) {
        printLogo();
        this.providerParameters = booterParameters;
        this.testClassLoader    = booterParameters.getTestClassLoader();
        this.runOrderCalculator = booterParameters.getRunOrderCalculator();
        this.providerProperties = booterParameters.getProviderProperties();
        this.testRequest        = booterParameters.getTestRequest();
        reporterConfiguration   = booterParameters.getReporterConfiguration();
        this.scanResult         = booterParameters.getScanResult();
    }

    public RunResult invoke(Object forkTestSet) throws TestSetFailedException, ReporterException {
        final ReporterFactory reporterFactory = providerParameters.getReporterFactory();
        if (testsToRun == null) {
            if (forkTestSet instanceof TestsToRun) {
                testsToRun = (TestsToRun)forkTestSet;
            } else if (forkTestSet instanceof Class) {
                testsToRun = TestsToRun.fromClass((Class)forkTestSet);
            } else {
                testsToRun = scanClassPath();
            }
        }
        //TODO: git user
        TestSuiteDirectory suite = getDirectorySuite();
        suite.execute(testsToRun, reporterFactory);
        return reporterFactory.close();
    }

    private TestSuiteDirectory getDirectorySuite() {
        return new TestSuiteDirectory(testRequest.getTestSourceDirectory().toString(), providerProperties, reporterConfiguration.getReportsDirectory(), testRequest.getTestSourceDirectory().toString(), runOrderCalculator, scanResult);
    }

    public Iterable<Class<?>> getSuites() {
        testsToRun = scanClassPath();
        return testsToRun;
    }

    private TestsToRun scanClassPath() {
        final TestsToRun scanned = scanResult.applyFilter(null, testClassLoader);
        return runOrderCalculator.orderTestClasses(scanned);
    }

    private void printLogo() {
        System.out.println("      ###########################################################################");
        System.out.println("      #              _________  __  ________  ________  ________                #");
        System.out.println("      #             /___  ___/ / / / ______/ / ______/ / ____  /                #");
        System.out.println("      #                / /    / /  \\ \\      / /_____  / /___/ /                 #");
        System.out.println("      #               / /    / /    \\ \\    / ______/ /  _  __/                  #");
        System.out.println("      #              / /    / / _____\\ \\  / /_____  / /  \\ \\                    #");
        System.out.println("      #             /_/    /_/ /_______/ /_______/ /_/    \\_\\                   #");
        System.out.println("      #                        Test Id Serializer                               #");
        System.out.println("      ###########################################################################");
    }
}
