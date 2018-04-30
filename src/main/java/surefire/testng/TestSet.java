package surefire.testng;

public class TestSet {

    private Class testClass;

    public TestSet(Class testClass) {
        if (testClass == null) {
            throw new NullPointerException("testClass is null");
        }
        this.testClass = testClass;
    }

    public String getName() {
        return testClass.getName();
    }

    public Class getTestClass() {
        return testClass;
    }
}
