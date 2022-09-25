/*
 * Copyright 2016 Nick Russler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
