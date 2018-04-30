package surefire.testng;

import org.apache.maven.surefire.booter.ProviderParameterNames;
import org.apache.maven.surefire.report.RunListener;
import org.apache.maven.surefire.testng.conf.Configurator;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.apache.maven.surefire.util.internal.StringUtils;
import org.testng.TestNG;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlMethodSelector;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Executor {

    // the classes are available in the testClassPath
    private static String CONFIGURATOR                  = "testng.configurator";
    private static String METHOD_SELECTOR               = "surefire.testng.MethodSelector";
    private static String GROUP_MATCHER_METHOD_SELECTOR = "surefire.testng.GroupMatcherMethodSelector";
    private static String CONFIGURATION_AWARE_REPORTER  = "surefire.testng.ConfigurationAwareReporter";
    private Executor() { }

    @SuppressWarnings("unchecked")
    public static void run(Class[] testClasses, String testSourceDirectory, Map options, RunListener reportManager, TestSuite suite, File reportsDirectory, final String methodNamePattern) throws TestSetFailedException {
        TestNG testng = new TestNG(true);
        Configurator configurator = getConfigurator((String)options.get(CONFIGURATOR));
        XmlMethodSelector groupMatchingSelector = getGroupMatchingSelector(options);
        XmlMethodSelector methodNameFilteringSelector = getMethodNameFilteringSelector(methodNamePattern);
        List<XmlSuite> suites = new ArrayList<XmlSuite>(testClasses.length);
        for (Class testClass : testClasses) {
            XmlSuite xmlSuite = new XmlSuite();
            xmlSuite.setName(testClass.getName());
            configurator.configure(xmlSuite, options);
            XmlTest xmlTest = new XmlTest(xmlSuite);
            xmlTest.setXmlClasses(Arrays.asList(new XmlClass(testClass)));
            addSelector(xmlTest, groupMatchingSelector);
            addSelector(xmlTest, methodNameFilteringSelector);
            suites.add(xmlSuite);
        }
        testng.setXmlSuites(suites);
        configurator.configure(testng, options);
        postConfigure(testng, testSourceDirectory, reportManager, suite, reportsDirectory);
        testng.run();
    }

    private static void addSelector(XmlTest xmlTest, XmlMethodSelector selector) {
        if (selector != null) {
            xmlTest.getMethodSelectors().add(selector);
        }
    }

    @SuppressWarnings("unchecked")
    private static XmlMethodSelector getMethodNameFilteringSelector(String methodNamePattern) throws TestSetFailedException {
        if (StringUtils.isBlank(methodNamePattern)) {
            return null;
        }

        try {
            Class clazz = Class.forName(METHOD_SELECTOR);
            Method method = clazz.getMethod("setMethodName", new Class[]{String.class});
            method.invoke(null, methodNamePattern);
        } catch (ClassNotFoundException e) {
            throw new TestSetFailedException(e.getMessage(), e);
        } catch (SecurityException e) {
            throw new TestSetFailedException(e.getMessage(), e);
        } catch (NoSuchMethodException e) {
            throw new TestSetFailedException(e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new TestSetFailedException(e.getMessage(), e);
        } catch (IllegalAccessException e) {
            throw new TestSetFailedException(e.getMessage(), e);
        } catch (InvocationTargetException e) {
            throw new TestSetFailedException(e.getMessage(), e);
        }
        XmlMethodSelector xms = new XmlMethodSelector();
        xms.setName(METHOD_SELECTOR);
        xms.setPriority(10000);  // looks to need a high value
        return xms;
    }

    @SuppressWarnings("unchecked")
    private static XmlMethodSelector getGroupMatchingSelector(Map options) throws TestSetFailedException {
        String groups = (String) options.get(ProviderParameterNames.TESTNG_GROUPS_PROP);
        String excludedGroups = (String) options.get(ProviderParameterNames.TESTNG_EXCLUDEDGROUPS_PROP);
        if (groups == null && excludedGroups == null) {
            return null;
        }

        try {
            Class clazz = Class.forName(GROUP_MATCHER_METHOD_SELECTOR);
            Method method = clazz.getMethod("setGroups", new Class[]{String.class, String.class}); // HORRIBLE hack, but TNG doesn't allow us to setup a method selector instance directly.
            method.invoke(null, groups, excludedGroups);
        } catch (ClassNotFoundException e) {
            throw new TestSetFailedException(e.getMessage(), e);
        } catch (SecurityException e) {
            throw new TestSetFailedException(e.getMessage(), e);
        } catch (NoSuchMethodException e) {
            throw new TestSetFailedException(e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new TestSetFailedException(e.getMessage(), e);
        } catch (IllegalAccessException e) {
            throw new TestSetFailedException(e.getMessage(), e);
        } catch (InvocationTargetException e) {
            throw new TestSetFailedException(e.getMessage(), e);
        }
        XmlMethodSelector xms = new XmlMethodSelector();
        xms.setName(GROUP_MATCHER_METHOD_SELECTOR);
        xms.setPriority(9999); // looks to need a high value
        return xms;
    }

    @SuppressWarnings("unchecked")
    public static void run(List<String> suiteFiles, String testSourceDirectory, Map options, RunListener reportManager, TestSuite suite, File reportsDirectory) throws TestSetFailedException {
        TestNG testng = new TestNG(true);
        Configurator configurator = getConfigurator((String)options.get(CONFIGURATOR));
        configurator.configure(testng, options);
        postConfigure(testng, testSourceDirectory, reportManager, suite, reportsDirectory);
        testng.setTestSuites(suiteFiles);
        testng.run();
    }

    private static Configurator getConfigurator(String className) {
        try {
            return (Configurator)Class.forName(className).newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("deprecation")
    private static void postConfigure(TestNG testNG, String sourcePath, RunListener reportManager, TestSuite suite, File reportsDirectory) {
        testNG.setVerbose(0); // turn off all TestNG output
        Reporter reporter = createTestNGReporter(reportManager, suite);
        testNG.addListener((Object) reporter);
        // FIXME: use classifier to decide if we need to pass along the source dir (onyl for JDK14)
        if (sourcePath != null) {
            testNG.setSourcePath(sourcePath);
        }
        testNG.setOutputDirectory(reportsDirectory.getAbsolutePath());
    }

    // If we have access to IResultListener, return a ConfigurationAwareReporter
    // But don't cause NoClassDefFoundErrors if it isn't available; just return a regular Reporter instead
    @SuppressWarnings("unchecked")
    private static Reporter createTestNGReporter(RunListener reportManager, TestSuite suite) {
        try {
            Class.forName("org.testng.internal.IResultListener");
            Class c = Class.forName(CONFIGURATION_AWARE_REPORTER);
            try {
                Constructor ctor = c.getConstructor(RunListener.class, TestSuite.class);
                return (Reporter) ctor.newInstance(reportManager, suite);
            } catch (Exception e) {
                throw new RuntimeException("Bug in ConfigurationAwareReporter", e);
            }
        } catch (ClassNotFoundException e) {
            return new Reporter(reportManager);
        }
    }

}
