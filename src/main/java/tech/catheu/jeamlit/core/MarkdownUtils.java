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
package tech.catheu.jeamlit.core;

import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.emoji.EmojiExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import com.vladsch.flexmark.util.misc.Extension;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MarkdownUtils {

    private static final Parser parser;
    private static final HtmlRenderer renderer;

    static {
        final MutableDataSet options = new MutableDataSet();
        final List<Extension> extensions = new ArrayList<>();
        extensions.add(TablesExtension.create());
        extensions.add(EmojiExtension.create());
        extensions.add(StrikethroughExtension.create());
        extensions.add(AutolinkExtension.create());
        options.set(Parser.EXTENSIONS, extensions);
        parser = Parser.builder(options).build();
        renderer = HtmlRenderer.builder(options).build();
    }

    protected static String markdownToHtml(final @Nullable String markdown) {
        if (markdown == null) {
            return null;
        }
        final Node document = parser.parse(markdown);
        return renderer.render(document);
    }
}
