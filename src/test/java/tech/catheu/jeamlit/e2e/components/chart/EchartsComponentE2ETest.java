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
package tech.catheu.jeamlit.e2e.components.chart;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import tech.catheu.jeamlit.e2e.helpers.PlaywrightUtils;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static tech.catheu.jeamlit.e2e.helpers.PlaywrightUtils.WAIT_1_SEC_MAX;

/**
 * End-to-end tests for EchartsComponent.
 */
public class EchartsComponentE2ETest {

    @Test
    void testEcharts_SimpleBarChart() {
        final @Language("java") String app = """
            import tech.catheu.jeamlit.core.Jt;
            import org.icepear.echarts.Bar;
            
            public class TestApp {
                public static void main(String[] args) {
                    Bar chart = new Bar()
                        .addXAxis(new String[]{"Mon", "Tue", "Wed"})
                        .addYAxis()
                        .addSeries("Sales", new Number[]{120, 200, 150});
                    
                    Jt.echarts(chart).use();
                }
            }
            """;

        PlaywrightUtils.runInSharedBrowser(app, page -> {
            // Wait for ECharts component to be visible
            assertThat(page.locator("jt-echarts")).isVisible(WAIT_1_SEC_MAX);
            
            // Check that canvas element exists (ECharts renders to canvas)
            assertThat(page.locator("jt-echarts canvas")).isVisible(WAIT_1_SEC_MAX);
        });
    }
}
