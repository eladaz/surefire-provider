package surefire.testng;

import org.apache.maven.surefire.group.match.AndGroupMatcher;
import org.apache.maven.surefire.group.match.GroupMatcher;
import org.apache.maven.surefire.group.match.InverseGroupMatcher;
import org.apache.maven.surefire.group.parse.GroupMatcherParser;
import org.apache.maven.surefire.group.parse.ParseException;
import org.testng.IMethodSelector;
import org.testng.IMethodSelectorContext;
import org.testng.ITestNGMethod;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupMatcherMethodSelector implements IMethodSelector {

    private static final long serialVersionUID = 1L;
    private static GroupMatcher matcher;
    private Map<ITestNGMethod, Boolean> answers = new HashMap<ITestNGMethod, Boolean>();

    @Override
    public boolean includeMethod(IMethodSelectorContext context, ITestNGMethod method, boolean isTestMethod) {
        Boolean result = (Boolean) answers.get(method);
        if (result != null) {
            return result;
        }
        if (matcher == null) {
            return true;
        }
        String[] groups = method.getGroups();
        result = Boolean.valueOf(matcher.enabled(groups));
        answers.put(method, result);
        return result;
    }

    @Override
    public void setTestMethods(List<ITestNGMethod> testMethods) { }

    public static void setGroups(String groups, String excludedGroups) {
        try {
            AndGroupMatcher matcher = new AndGroupMatcher();
            GroupMatcher in = null;
            if (groups != null && groups.trim().length() > 0) {
                in = new GroupMatcherParser(groups).parse();
            }

            if (in != null) {
                matcher.addMatcher(in);
            }

            GroupMatcher ex = null;
            if (excludedGroups != null && excludedGroups.trim().length() > 0) {
                ex = new GroupMatcherParser(excludedGroups).parse();
            }

            if (ex != null) {
                matcher.addMatcher(new InverseGroupMatcher(ex));
            }

            if (in != null || ex != null) {
                GroupMatcherMethodSelector.matcher = matcher;
            }
        } catch (ParseException e) {
            throw new IllegalArgumentException("Cannot parse group includes/excludes expression(s):\nIncludes: " + groups + "\nExcludes: " + excludedGroups, e);
        }
    }

    public static void setGroupMatcher(GroupMatcher matcher) {
        GroupMatcherMethodSelector.matcher = matcher;
    }
}
