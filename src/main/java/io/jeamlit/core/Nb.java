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
package io.jeamlit.core;

import java.io.StringWriter;
import java.util.Map;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

public class Nb {

    private static final Mustache initTemplate;

    static {
        final MustacheFactory mf = new DefaultMustacheFactory();
        initTemplate = mf.compile("nb.html.mustache");
    }

    public String getInitHtml() {
        final StringWriter writer = new StringWriter();
        initTemplate.execute(writer, Map.of("MATERIAL_SYMBOLS_CDN",
                                            JtComponent.MATERIAL_SYMBOLS_CDN,
                                            "LIT_DEPENDENCY",
                                            JtComponent.LIT_DEPENDENCY,
                                            "PRISM_SETUP_SNIPPET",
                                            JtComponent.PRISM_SETUP_SNIPPET,
                                            "PRISM_CSS",
                                            JtComponent.PRISM_CSS,
                                            "MARKDOWN_CSS", JtComponent.MARKDOWN_CSS));
        return writer.toString();
    }

    public String render(JtComponent component) {
        return component.register() + component.render();
    }
}
