package surefire.testng;

import org.apache.maven.surefire.shade.org.apache.maven.shared.utils.io.SelectorUtils;
import org.testng.IMethodSelector;
import org.testng.IMethodSelectorContext;
import org.testng.ITestNGMethod;

import java.util.List;

public class MethodSelector implements IMethodSelector {

    private static String METHOD_NAME = null;

    @Override
    public boolean includeMethod(IMethodSelectorContext context, ITestNGMethod method, boolean isTestMethod) {
        if (method.isBeforeClassConfiguration()
         || method.isBeforeGroupsConfiguration()
         || method.isBeforeMethodConfiguration()
         || method.isBeforeSuiteConfiguration()
         || method.isBeforeTestConfiguration()) {
            return true;
        }
        if (method.isAfterClassConfiguration()
         || method.isAfterGroupsConfiguration()
         || method.isAfterMethodConfiguration()
         || method.isAfterSuiteConfiguration()
         || method.isAfterTestConfiguration()) {
            return true;
        }
        return SelectorUtils.match(METHOD_NAME, method.getMethodName());
    }

    @Override
    public void setTestMethods(List<ITestNGMethod> testMethods) { }

    public static void setMethodName(String methodName) {
        METHOD_NAME = methodName;
    }
}
