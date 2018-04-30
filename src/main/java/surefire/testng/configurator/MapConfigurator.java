package surefire.testng.configurator;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.maven.surefire.booter.ProviderParameterNames;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.testng.TestNG;
import org.testng.xml.XmlSuite;

public class MapConfigurator implements Configurator {
    @SuppressWarnings("deprecation")
    public void configure(TestNG testng, Map options) throws TestSetFailedException {
        Map convertedOptions = getConvertedOptions(options);
        testng.configure(convertedOptions);
    }

    @SuppressWarnings("deprecation")
    public void configure(XmlSuite suite, Map options) {
        String threadCountString = (String) options.get(ProviderParameterNames.THREADCOUNT_PROP);
        int threadCount = (null != threadCountString) ? Integer.parseInt(threadCountString) : 1;
        suite.setThreadCount(threadCount);
        String parallel = (String) options.get(ProviderParameterNames.PARALLEL_PROP);
        if (parallel != null) {
            suite.setParallel(parallel);
        }
    }

    @SuppressWarnings("unchecked")
    Map getConvertedOptions(Map options) throws TestSetFailedException {
        Map convertedOptions = new HashMap();
        convertedOptions.put("-mixed", Boolean.FALSE);
        for (Iterator it = options.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();
            String key = (String) entry.getKey();
            Object val = entry.getValue();
            if ("listener".equals(key)) {
                val = AbstractDirectConfigurator.loadListenerClasses((String) val);
            }
            if ("objectfactory".equals(key)) {
                val = AbstractDirectConfigurator.loadClass((String) val);
            }
            if ("reporter".equals(key)) {
                // TODO support multiple reporters?
                val = convertReporterConfig(val);
                key = "reporterslist";
            }
            if ("junit".equals(key)) {
                val = convert(val, Boolean.class);
            } else if ("skipfailedinvocationcounts".equals(key)) {
                val = convert(val, Boolean.class);
            } else if ("mixed".equals(key)) {
                val = convert(val, Boolean.class);
            } else if ("configfailurepolicy".equals(key)) {
                val = convert(val, String.class);
            } else if ("group-by-instances".equals(key)) {
                val = convert(val, Boolean.class);
            } else if (ProviderParameterNames.THREADCOUNT_PROP.equals(key)) {
                val = convert(val, String.class);
            }
            // TODO objectfactory... not even documented, does it work?
            if (key.startsWith("-")) {
                convertedOptions.put(key, val);
            } else {
                convertedOptions.put("-" + key, val);
            }
        }
        return convertedOptions;
    }

    // ReporterConfig only became available in later versions of TestNG
    @SuppressWarnings("unchecked")
    private Object convertReporterConfig(Object val) {
        final String reporterConfigClassName = "org.testng.ReporterConfig";
        try {
            Class reporterConfig = Class.forName(reporterConfigClassName);
            Method deserialize = reporterConfig.getMethod("deserialize", new Class[]{String.class});
            Object rc = deserialize.invoke(null, new Object[]{val});
            ArrayList reportersList = new ArrayList();
            reportersList.add(rc);
            return reportersList;
        } catch (Exception e) {
            return val;
        }
    }

    @SuppressWarnings("unchecked")
    protected Object convert(Object val, Class type) {
        if (val == null) {
            return null;
        }
        if (type.isAssignableFrom(val.getClass())) {
            return val;
        }
        if ((Boolean.class.equals(type) || boolean.class.equals(type)) && String.class.equals(val.getClass())) {
            return Boolean.valueOf((String) val);
        }
        if (String.class.equals(type)) {
            return val.toString();
        }
        return val;
    }
}