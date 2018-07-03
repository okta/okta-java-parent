/*
 * Copyright 2018-Present Okta, Inc.
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
package com.okta.maven.snippet;

public class Example {

    public void doFoo() {
        System
          .out.println("doing foo");
        int result = 1 + 1;
    }

    private void andBar(String value) {
        // with a comment
        String another = value + "bar";

        // another comment
        int result = 2 + 2;

    }
}
