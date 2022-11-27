package de.dwienzek.emailtopaperless.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class StringReplacer {

    public static String replace(String input, Pattern regex, Function<Matcher, String> function) {
        StringBuilder resultString = new StringBuilder();
        Matcher regexMatcher = regex.matcher(input);

        while (regexMatcher.find()) {
            regexMatcher.appendReplacement(resultString, Matcher.quoteReplacement(function.apply(regexMatcher)));
        }

        regexMatcher.appendTail(resultString);
        return resultString.toString();
    }

}