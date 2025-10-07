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
package io.jeamlit.cli;

import io.jeamlit.core.Jt;

public class HelloWorld {

    public static void main(String[] args) {
        Jt.title("Welcome to Jeamlit! \uD83D\uDEA1").use();
        Jt.markdown("Jeamlit is an open-source Java app framework built specifically for fast app development.   "
                    + "Build your next data app, back-office, internal tool, or demo with Jeamlit!").use();
        Jt.markdown("""
                            ## Want to learn more?
                            - Check out [jeamlit.io](https://jeamlit.io)
                            - Jump into our [documentation](https://docs.jeamlit.io)
                            - Ask a question in the [community forum](https://github.com/jeamlit/jeamlit/discussions)
                            """).use();

    }
}
