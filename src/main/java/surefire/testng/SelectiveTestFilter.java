package surefire.testng;


import db.postgres.ddtelements.Test;
import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

public class SelectiveTestFilter extends Filter {

    private Filter filter;
    private HashMap<String, Test> map;
    private static int filteredTestsNumber = 0;

    public static int getFilteredTestsNumber() {
        return filteredTestsNumber;
    }

    public SelectiveTestFilter(Filter filter){
        this.filter = filter;
    }

    private String _trimTeam(String testName) {
        if(testName != null && testName.indexOf("[") > 0) {
            testName = testName.substring(0,testName.indexOf("[")).trim();
        }
        return testName;
    }

    @Override
    public boolean shouldRun(Description description) {
        boolean isSelectedToRun = false;
        if (description.getMethodName() == null) {
            //Its a class filter, check if at least one test from the class is selected
            Collection<Test> tests = map.values();
            Iterator<Test> it = tests.iterator();
            Test test = null;
            while (!isSelectedToRun && it.hasNext()){
                test = it.next();
                isSelectedToRun = (test.toString().compareTo(description.getClassName()) == 0);
            }
            System.out.println("TISER Surefire Provider: Test Class [" + description.getClassName() + "] " + (isSelectedToRun ? "included" : "excluded"));
        } else {
            //Its a test filter. check if its exist in the map
            isSelectedToRun = map.containsKey(description.getClassName() + "#" + _trimTeam(description.getMethodName()));
            if (isSelectedToRun){
                filteredTestsNumber++;
            }
            System.out.println("TISER Surefire Provider: Test [" + description.getClassName() + "#" + description.getMethodName() + "], " + (isSelectedToRun ? "included" : "excluded"));
        }
        return isSelectedToRun && (filter == null || filter.shouldRun(description));
    }

    @Override
    public String describe() {
        return "TISER Surefire Provider - Collect all test id's";
    }

}
