package com.insidious.plugin.util;

import com.insidious.plugin.assertions.AtomicAssertion;

import java.util.ArrayList;
import java.util.List;

public class AtomicAssertionUtils {

    public static List<AtomicAssertion> flattenAssertionMap(AtomicAssertion testAssertions) {

        List<AtomicAssertion> all = new ArrayList<>();
        all.add(testAssertions);

        if (testAssertions.getSubAssertions() != null) {
            for (AtomicAssertion subAssertion : testAssertions.getSubAssertions()) {
                all.addAll(flattenAssertionMap(subAssertion));
            }
        }

        return all;
    }

    public static int countAssertions(AtomicAssertion testAssertions) {

        int count = 1;

        if (testAssertions.getSubAssertions() != null) {
            for (AtomicAssertion subAssertion : testAssertions.getSubAssertions()) {
                count += countAssertions(subAssertion);
            }
        }

        return count;
    }


}
