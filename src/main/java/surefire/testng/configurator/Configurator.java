package surefire.testng.configurator;

import java.util.Map;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.testng.TestNG;
import org.testng.xml.XmlSuite;

public interface Configurator {

    void configure(TestNG testng, Map options) throws TestSetFailedException;
    void configure(XmlSuite suite, Map options) throws TestSetFailedException;

}