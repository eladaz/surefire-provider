package surefire.testng;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import db.postgres.PostgresClient;

import db.postgres.ddtelements.Test;
import db.postgres.ddtelements.TestPermutation;
import org.apache.maven.surefire.NonAbstractClassFilter;
import org.apache.maven.surefire.report.*;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.apache.maven.surefire.util.RunOrderCalculator;
import org.apache.maven.surefire.util.ScanResult;
import org.apache.maven.surefire.util.TestsToRun;


@SuppressWarnings("unchecked")
public class TestSuiteDirectory implements TestSuite {

    private final Map                        options;
    private final Map                        junitOptions;
    private final String                     testSourceDirectory;
    private final String                     testMethodPattern;
    private       SortedMap<String, TestSet> testSets;
    private final File                       reportsDirectory;
    private final ScanResult                 scanResult;
    private final RunOrderCalculator         runOrderCalculator;
    private final Class                      junitTestClass;
    private Class<? extends Annotation>      junitRunWithAnnotation;
    private Class<? extends Annotation>      junitTestAnnotation;
    private PostgresClient postgres = PostgresClient.getInstance();

    public TestSuiteDirectory(String testSourceDirectory, Map<String, String> confOptions, File reportsDirectory, String testMethodPattern, RunOrderCalculator runOrderCalculator, ScanResult scanResult) {
        this.runOrderCalculator     = runOrderCalculator;
        this.options                = confOptions;
        this.testSourceDirectory    = testSourceDirectory;
        this.reportsDirectory       = reportsDirectory;
        this.scanResult             = scanResult;
        this.testMethodPattern      = testMethodPattern;
        this.junitTestClass         = findJUnitTestClass();
        this.junitRunWithAnnotation = findJUnitRunWithAnnotation();
        this.junitTestAnnotation    = findJUnitTestAnnotation();
        this.junitOptions           = createJUnitOptions();
    }

    public void execute(TestsToRun testsToRun, ReporterFactory reporterManagerFactory) throws ReporterException, TestSetFailedException {
        if (!testsToRun.allowEagerReading()) {
            executeLazy(testsToRun, reporterManagerFactory);
        } else if (testsToRun.containsAtLeast(2)) {
            executeMulti(testsToRun, reporterManagerFactory);
        } else if (testsToRun.containsAtLeast(1)) {
            Class testClass = testsToRun.iterator().next();
            executeSingleClass(reporterManagerFactory, testClass);
        }
    }

    private void executeSingleClass(ReporterFactory reporterManagerFactory, Class testClass) throws TestSetFailedException {
        this.options.put("suitename", testClass.getName());
        RunListener reporter = reporterManagerFactory.createReporter();
        ConsoleOutputCapture.startCapture((ConsoleOutputReceiver) reporter);
        startTestSuite(reporter, this);
        final Map optionsToUse = isJUnitTest(testClass) ? junitOptions : options;
        Executor.run(new Class[]{testClass}, testSourceDirectory, optionsToUse, reporter, this, reportsDirectory, testMethodPattern);
        finishTestSuite(reporter, this);
    }

    private void executeLazy(TestsToRun testClasses, ReporterFactory reporterFactory) throws ReporterException {
        System.out.println("\n");

        List<Test> tests = new ArrayList<>();
        for (Class testClass : testClasses) {
            try {
                executeSingleClass(reporterFactory, testClass);
            } catch (TestSetFailedException e) {
                e.printStackTrace();
            }
            tests.addAll(Test.getList(testClass));
        }

        postgres.insertTests(tests);
        List<TestPermutation> testPermutations = TestPermutation.getList(tests);
        postgres.insertTestPermutations(testPermutations);
    }

    private Class findJUnitTestClass() {
        return lookupClass("junit.framework.Test");
    }

    @SuppressWarnings("unchecked")
    private Class findJUnitRunWithAnnotation() {
        return lookupClass("org.junit.runner.RunWith");
    }

    private Class findJUnitTestAnnotation() {
        return lookupClass("org.junit.Test");
    }

    private Class lookupClass(String className) {
        Class junitClass;
        try {
            junitClass = Class.forName(className);
        } catch (ClassNotFoundException e) {
            junitClass = null;
        }
        return junitClass;
    }

    private void executeMulti(TestsToRun testsToRun, ReporterFactory reporterFactory) throws ReporterException, TestSetFailedException {
        List<Class> testNgTestClasses = new ArrayList<Class>();
        List<Class> junitTestClasses = new ArrayList<Class>();
        for (Class testClass : testsToRun) {
            if (isJUnitTest(testClass)) {
                junitTestClasses.add(testClass);
            } else {
                testNgTestClasses.add(testClass);
            }
        }

        File testNgReportsDirectory = reportsDirectory, junitReportsDirectory = reportsDirectory;

        if (junitTestClasses.size() > 0 && testNgTestClasses.size() > 0) {
            testNgReportsDirectory = new File(reportsDirectory, "testng-native-results");
            junitReportsDirectory = new File(reportsDirectory, "testng-junit-results");
        }

        RunListener reporterManager = reporterFactory.createReporter();
        ConsoleOutputCapture.startCapture((ConsoleOutputReceiver) reporterManager);
        startTestSuite(reporterManager, this);

        Class[] testClasses = testNgTestClasses.toArray(new Class[testNgTestClasses.size()]);
        Executor.run(testClasses, this.testSourceDirectory, options, reporterManager, this, testNgReportsDirectory, testMethodPattern);
        if (junitTestClasses.size() > 0) {
            testClasses = junitTestClasses.toArray(new Class[junitTestClasses.size()]);
            Executor.run(testClasses, testSourceDirectory, junitOptions, reporterManager, this, junitReportsDirectory, testMethodPattern);
        }
        finishTestSuite(reporterManager, this);
    }

    private boolean isJUnitTest(Class testClass) {
        return isJunit3Test(testClass) || isJunit4Test(testClass);
    }

    private boolean isJunit4Test(Class testClass) {
        return hasJunit4RunWithAnnotation(testClass) || hasJunit4TestAnnotation(testClass);
    }

    private boolean hasJunit4RunWithAnnotation(Class testClass) {
        return junitRunWithAnnotation != null && testClass.getAnnotation(junitRunWithAnnotation) != null;
    }

    private boolean hasJunit4TestAnnotation(Class testClass) {
        if (junitTestAnnotation != null) {
            for (Method method : testClass.getMethods()) {
                if (method.getAnnotation(junitTestAnnotation) != null) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isJunit3Test(Class c) {
        return junitTestClass != null && junitTestClass.isAssignableFrom(c);
    }

    private Map createJUnitOptions() {
        Map junitOptions = new HashMap(this.options);
        junitOptions.put("junit", Boolean.TRUE);
        return junitOptions;
    }

    // single class test
    public void execute(String testSetName, ReporterFactory reporterManagerFactory) throws ReporterException, TestSetFailedException {
        if (testSets == null) {
            throw new IllegalStateException("You must call locateTestSets before calling execute");
        }
        TestSet testSet = testSets.get(testSetName);
        if (testSet == null) {
            throw new TestSetFailedException("Unable to find test set '" + testSetName + "' in suite");
        }
        RunListener reporter = reporterManagerFactory.createReporter();
        ConsoleOutputCapture.startCapture((ConsoleOutputReceiver) reporter);
        startTestSuite(reporter, this);
        Executor.run(new Class[]{testSet.getTestClass()}, this.testSourceDirectory, this.options, reporter, this, reportsDirectory, testMethodPattern);
        finishTestSuite(reporter, this);
    }

    private static void startTestSuite(RunListener reporter, Object suite) {
        ReportEntry report = new SimpleReportEntry(suite.getClass().getName(), getSuiteName(suite));
        try {
            reporter.testSetStarting((TestSetReportEntry) report);
        } catch (ReporterException e) {
            // TODO: remove this exception from the report manager
        }
    }

    private static void finishTestSuite(RunListener reporterManager, Object suite) throws ReporterException {
        ReportEntry report = new SimpleReportEntry(suite.getClass().getName(), getSuiteName(suite));
        reporterManager.testSetCompleted((TestSetReportEntry)report);
    }

    private String getSuiteName() {
        String result = (String)options.get("suitename");
        return result == null ? "TestSuite" : result;
    }

    private static String getSuiteName(Object suite) {
        return suite instanceof TestSuiteDirectory ? ((TestSuiteDirectory)suite).getSuiteName() : "TestSuite";
    }

    public Map locateTestSets(ClassLoader classLoader) throws TestSetFailedException {
        if (testSets != null) {
            throw new IllegalStateException("You can't call locateTestSets twice");
        }
        testSets = new TreeMap<String, TestSet>();
        final TestsToRun scanned = scanResult.applyFilter(new NonAbstractClassFilter(), classLoader);
        final TestsToRun testsToRun = runOrderCalculator.orderTestClasses(scanned);
        for (Class testClass : testsToRun) {
            TestSet testSet = new TestSet(testClass);
            if (testSets.containsKey(testSet.getName())) {
                throw new TestSetFailedException("Duplicate test set '" + testSet.getName() + "'");
            }
            testSets.put(testSet.getName(), testSet);
        }
        return Collections.unmodifiableSortedMap(testSets);
    }

}
