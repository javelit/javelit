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

import java.util.ArrayList;
import java.util.List;

import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.emoji.EmojiExtension;
import com.vladsch.flexmark.ext.emoji.EmojiImageType;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import com.vladsch.flexmark.util.misc.Extension;
import jakarta.annotation.Nullable;

public final class MarkdownUtils {

    private static final Parser parser;
    private static final HtmlRenderer renderer;

    static {
        final MutableDataSet options = new MutableDataSet();
        final List<Extension> extensions = new ArrayList<>();
        extensions.add(TablesExtension.create());
        extensions.add(EmojiExtension.create());
        extensions.add(StrikethroughExtension.create());
        extensions.add(AutolinkExtension.create());
        options.set(EmojiExtension.USE_IMAGE_TYPE, EmojiImageType.UNICODE_ONLY);
        options.set(Parser.EXTENSIONS, extensions);
        parser = Parser.builder(options).build();
        renderer = HtmlRenderer.builder(options).build();
    }

    /// @param removeWrap if true, removes the wrapping tag if it exists. Usefull to remove the wrapping <p> tag that is put to text like "this text". If false, no post-processing. The wrapping tag removed could be any type of tag as long as it's wrapping, ie it's open first and closed last in the string.
    static String markdownToHtml(final @Nullable String markdown, final boolean removeWrap) {
        if (markdown == null) {
            return null;
        }
        final Node document = parser.parse(markdown);
        String html =  renderer.render(document).trim();
        if (removeWrap) {
            final boolean thereIsAWrappingTag = document.getFirstChild() == document.getLastChild();
            if (thereIsAWrappingTag) {
                final int closeWrapTagIdx = html.indexOf('>');
                html = html.substring(closeWrapTagIdx + 1, html.length() - closeWrapTagIdx - 2).trim();
            }
        }
        return html;
    }

    private MarkdownUtils() {
    }
}
