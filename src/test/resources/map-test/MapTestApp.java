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


import java.net.URI;
import java.util.Map;

import io.javelit.components.chart.EchartsComponent;
import io.javelit.core.Jt;

public class MapTestApp {

  public static void main(String[] args) {
    Jt.title("Map Test Application").use();
    Jt.text("This is a test application for ECharts map functionality").use();

    // Define special areas for testing
    Map<String, EchartsComponent.SpecialAreaConfig> specialAreas = Map.of(
        "Alaska", new EchartsComponent.SpecialAreaConfig(-131, 25, 15),
        "Hawaii", new EchartsComponent.SpecialAreaConfig(-110, 28, 5)
    );

    // Simple map test using JSON option
    String mapOption = "{\"title\": {\"text\": \"Test Map\"}, \"series\": [{\"name\": \"Test\", \"type\": \"map\", \"map\": \"USA\", \"data\": [[\"California\", 100], [\"Texas\", 200]]}]}";

    // Create chart with USA map using local test resource
    Jt.echarts(mapOption)
      .withMap("USA", URI.create("/app/map-test/static/usa.json"))
      .height(400)
      .use();

    Jt.markdown("---").use();
    Jt.text("Map loaded from test resources: /app/map-test/static/usa.json").use();

    // Test with special areas
    Jt.text("Testing with special areas configuration:").use();

    String mapOptionWithAreas = "{\"title\": {\"text\": \"Map with Special Areas\"}, \"series\": [{\"name\": \"Test Areas\", \"type\": \"map\", \"map\": \"USA\", \"data\": [[\"Alaska\", 50], [\"Hawaii\", 25]]}]}";

    Jt.echarts(mapOptionWithAreas)
      .withMap("USA", URI.create("/app/map-test/static/usa.json"), specialAreas)
      .height(400)
      .use();
  }
}
