/*
 * Copyright 2023-Present Okta, Inc.
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
package com.okta.maven.doclist;

import java.util.Comparator;

class SemanticVersionComparator implements Comparator<String> {

    // split a String at any '.' or '-', and remove anything after a '+' (metadata)
    // [major,minor,patch,label]
    private String[] splitSemanticVersion(String semanticVersion) {
        int metaDataStart = semanticVersion.indexOf("+") < 0 ? semanticVersion.length() : semanticVersion.indexOf("+");
        return semanticVersion.substring(0, metaDataStart).split("[.|-]");
    }

    @Override
    public int compare(String semanticVersion1, String semanticVersion2) {
        String[] semanticVersion1Parts = splitSemanticVersion(semanticVersion1);
        String[] semanticVersion2Parts = splitSemanticVersion(semanticVersion2);

        // compare major versions
        int majorComparison = Integer.compare(Integer.parseInt(semanticVersion1Parts[0]), Integer.parseInt(semanticVersion2Parts[0]));
        if (majorComparison != 0)
            return majorComparison;

        // compare minor versions
        int minorComparison = Integer.compare(Integer.parseInt(semanticVersion1Parts[1]), Integer.parseInt(semanticVersion2Parts[1]));
        if (minorComparison != 0)
            return minorComparison;

        // compare patches
        int patchComparison = Integer.compare(Integer.parseInt(semanticVersion1Parts[2]), Integer.parseInt(semanticVersion2Parts[2]));
        if (patchComparison != 0)
            return patchComparison;

        // check for the presence of a label (e.g. '-beta')
        return Integer.compare(semanticVersion2Parts.length, semanticVersion1Parts.length);
    }
}
