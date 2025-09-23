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
package io.jeamlit.components.chart;

import java.io.StringWriter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import io.jeamlit.core.JtComponent;
import io.jeamlit.core.JtComponentBuilder;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.icepear.echarts.Chart;
import org.icepear.echarts.Option;
import org.icepear.echarts.serializer.EChartsSerializer;
import org.intellij.lang.annotations.Language;

public class EchartsComponent extends JtComponent<JtComponent.NONE> {

    public enum Theme {
        DEFAULT("default"),
        CHALK("chalk"),
        DARK("dark"),
        ESSOS("essos"),
        HALLOWEEN("halloween"),
        INFOGRAPHIC("infographic"),
        MACARONS("macarons"),
        PURPLE_PASSION("purple-passion"),
        ROMA("roma"),
        SHINE("shine"),
        VINTAGE("vintage"),
        WALDEN("walden"),
        WESTEROS("westeros"),
        WONDERLAND("wonderland");

        private final String name;

        Theme(final @Nonnull String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }


    private static final Mustache registerTemplate;
    private static final Mustache renderTemplate;

    protected final @Language("json") String echartOption;
    protected final int height;
    protected final @Nullable Integer width;
    protected final String theme;

    static {
        final MustacheFactory mf = new DefaultMustacheFactory();
        registerTemplate = mf.compile("components/chart/EchartsComponent.register.html.mustache");
        renderTemplate = mf.compile("components/chart/EchartsComponent.render.html.mustache");
    }

    private EchartsComponent(final @Nonnull Builder builder) {
        super(builder.generateKeyForInteractive(), NONE.NONE_VALUE, null);
        if (builder.chart != null) {
            // see https://github.com/ECharts-Java/ECharts-Java/blob/61c82301c5fb45bb848cb779640968f5b959955e/src/main/java/org/icepear/echarts/render/Engine.java#L184C12-L184C18
            // we are not using Engine.java directly because it import handlebars - we don't want handlebars as a dependency, it's excluded
            this.echartOption = new EChartsSerializer().toJson(builder.chart.getOption());
        } else if (builder.option != null) {
            // see comment above
            this.echartOption = new EChartsSerializer().toJson(builder.option);
        } else if (builder.optionJson != null) {
            this.echartOption = builder.optionJson;
        } else {
            throw new IllegalArgumentException("One of chart, option, json must be set. Please reach out to support.");
        }
        this.height = builder.height;
        this.width = builder.width;
        this.theme = builder.theme;
    }

    public static class Builder extends JtComponentBuilder<JtComponent.NONE, EchartsComponent, Builder> {
        private @Nullable final Chart<?, ?> chart;
        private @Nullable final Option option;
        private @Nullable @Language("json") final String optionJson;
        private int height = 400;
        private Integer width;
        private String theme = "default";

        public Builder(final @Nonnull Chart<?, ?> chart) {
            this.chart = chart;
            this.option = null;
            this.optionJson = null;
        }

        public Builder(final @Nonnull Option option) {
            this.chart = null;
            this.option = option;
            this.optionJson = null;
        }

        public Builder(final @Nonnull @Language("json") String optionJson) {
            this.chart = null;
            this.option = null;
            this.optionJson = optionJson;
        }

        /**
         * The height of the chart in pixels.
         */
        public Builder height(final int height) {
            this.height = height;
            return this;
        }

        /**
         * The width of the chart in pixels. If null, the chart will use its default width.
         */
        public Builder width(final @Nullable Integer width) {
            this.width = width;
            return this;
        }

        /**
         * The chart theme using a predefined theme from the Theme enum.
         */
        public Builder theme(final @Nonnull Theme theme) {
            return theme(theme.toString());
        }

        /**
         * The chart theme using a custom theme name. Custom themes can be loaded through custom headers.
         */
        public Builder theme(final @Nonnull String theme) {
            // no checks - custom headers may import some custom theme - custom themes are not supported any other way for the moment
            this.theme = theme;
            return this;
        }

        @Override
        public EchartsComponent build() {
            return new EchartsComponent(this);
        }
    }

    @Override
    protected String register() {
        final StringWriter writer = new StringWriter();
        registerTemplate.execute(writer, this);
        return writer.toString();
    }

    @Override
    protected String render() {
        final StringWriter writer = new StringWriter();
        renderTemplate.execute(writer, this);
        return writer.toString();
    }

    @Override
    protected TypeReference<NONE> getTypeReference() {
        return new TypeReference<>() {
        };
    }
}
