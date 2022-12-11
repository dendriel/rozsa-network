package com.rozsa.test;

import java.util.Arrays;

public class ArgsUtils {

    public static boolean getBoolean(final String[] args, final String key) {
        String[] tokens = Arrays.stream(args)
                .filter(a -> a.contains(key + "="))
                .findFirst()
                .orElse(key+"=false")
                .split("=");

        if (tokens.length < 2) {
            return false;
        }

        return Boolean.parseBoolean(tokens[1]);
    }
}
