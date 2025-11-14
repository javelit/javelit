/*
 * Copyright © 2025 Cyril de Catheu (cdecatheu@hey.com)
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
///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.javelit:javelit:0.67.0

import io.javelit.core.Jt;

public class TextExample {

    public static void main(String[] args) {
        Jt.text("Hello, World!").use();
        Jt.text("This is a simple text example.").use();
    }
}
