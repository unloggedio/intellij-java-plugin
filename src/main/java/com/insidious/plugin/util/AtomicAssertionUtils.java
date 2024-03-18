package com.insidious.plugin.util;

import com.insidious.plugin.assertions.AssertionType;
import com.insidious.plugin.assertions.AtomicAssertion;

import java.util.ArrayList;
import java.util.List;

public class AtomicAssertionUtils {

    public static List<AtomicAssertion> flattenAssertionMap(AtomicAssertion testAssertions) {

        List<AtomicAssertion> all = new ArrayList<>();
        if (testAssertions == null) {
            return all;
        }
        all.add(testAssertions);

        if (testAssertions.getSubAssertions() != null) {
            for (AtomicAssertion subAssertion : testAssertions.getSubAssertions()) {
                all.addAll(flattenAssertionMap(subAssertion));
            }
        }

        return all;
    }

    public static int countAssertions(AtomicAssertion testAssertions) {
        if (testAssertions == null) {
            return 0;
        }

        if (testAssertions.getSubAssertions() == null || testAssertions.getSubAssertions().size() == 0) {
            if (testAssertions.getAssertionType() == AssertionType.ALLOF
                    || testAssertions.getAssertionType() == AssertionType.ANYOF
                    || testAssertions.getAssertionType() == AssertionType.NOTALLOF
                    || testAssertions.getAssertionType() == AssertionType.NOTANYOF
            ) {
                return 0;
            }
            return 1;
        }

        int count = 0;

        for (AtomicAssertion subAssertion : testAssertions.getSubAssertions()) {
            count += countAssertions(subAssertion);
        }

        return count;
    }


}
