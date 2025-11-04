///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS io.javelit:javelit:0.64.0
//DEPS tech.tablesaw:tablesaw-core:0.44.4

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.zip.GZIPInputStream;

import io.javelit.core.Jt;
import org.icepear.echarts.Bar;
import org.icepear.echarts.Option;
import org.icepear.echarts.charts.scatter.ScatterSeries;
import org.icepear.echarts.components.coord.cartesian.CategoryAxis;
import org.icepear.echarts.components.series.ItemStyle;
import org.icepear.echarts.components.tooltip.Tooltip;
import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.selection.Selection;


public class App {

    public static void main(String[] args) {
        // Load data with caching
        Table data = (Table) Jt.cache().computeIfAbsent("data", k -> {
            Jt.text("Loading data...").use();
            Table df = loadData(10000);
            return df;
        });
        Jt.text("Loading data...done! (using Jt.cache())").use();

        // Title
        Jt.title("Uber pickups in NYC").use();

        // Checkbox to show raw data
        if (Jt.checkbox("Show raw data").use()) {
            Jt.markdown("## Raw data").use();
            Jt.table(data.first(6)).use();
        }

        // Create histogram data for pickups by hour
        Jt.markdown("## Number of pickups by hour").use();


        // Count pickups for each hour - counts is a dataframe with columns Category and Count
        Table counts = data.intColumn("hour").countByCategory().sortOn("Category");

        // Create bar chart using java ECharts - see https://echarts.icepear.org/#/chart-apis/bar
        Bar barChart = new Bar()
                .setTooltip(new Tooltip().setTrigger("axis"))
                .addXAxis(new CategoryAxis().setData(counts.intColumn("Category").asObjectArray()))
                .addYAxis()
                .addSeries(counts.intColumn("Count").asObjectArray());
        // plot the bar chart
        Jt.echarts(barChart).height(400).use();

        // Slider for hour selection
        int hourToFilter = Jt.slider("Select an hour to show on the map").min(0).max(23).value(17).use().intValue();

        // Filter data based on selected hour
        Table filteredData = data.where(data.intColumn("hour").isEqualTo(hourToFilter));

        // Display filtered data on map
        Jt.markdown("**Map of pickups at %d:00**".formatted(hourToFilter)).use();

        // Create map visualization
        // wrangle data for plotting --> list of points, with each point a {"value": [lon, lat]} - see https://echarts.apache.org/examples/en/editor.html?c=geo-graph
        List<Double> lons = filteredData.doubleColumn("lon").asList();
        List<Double> lats = filteredData.doubleColumn("lat").asList();
        List<Map<String, Double[]>> plotData = IntStream.range(0, lons.size())
                         .mapToObj(i -> Map.of("value", new Double[]{lons.get(i), lats.get(i)}))
                         .toList();
        // map chart config
        Option mapOption = new Option()
                .setGeo(Map.of("map", "manhattan",
                               "roam", true,
                               "zoom", 1.5,
                               "center", new Double[]{-73.98, 40.75},
                               "scaleLimit", Map.of("min", 1, "max", 10),
                               "tooltip", Map.of("show", true)))
                .setSeries(new ScatterSeries()
                                   .setData(plotData.toArray())
                                   .setCoordinateSystem("geo")
                                   .setSymbolSize(5)
                                   .setItemStyle(new ItemStyle().setColor("#b02a02").setOpacity(0.6)));
        // plot the chart
        String mapBaseGeoJson = "https://raw.githubusercontent.com/javelit/public_assets/refs/heads/main/examples/manhattan.geo.json";
        Jt.echarts(mapOption).withMap("manhattan", URI.create(mapBaseGeoJson)).height(600).border(true).use();
    }

    static Table loadData(int nrows) {
        final String DATE_COLUMN = "date/time";
        final String DATA_URL = "https://github.com/javelit/public_assets/raw/refs/heads/main/examples/uber-raw-data-sep14.csv.gz";
        try (InputStream in = URI.create(DATA_URL).toURL().openStream();
             GZIPInputStream gzipIn = new GZIPInputStream(in);
             InputStreamReader reader = new InputStreamReader(gzipIn);
             BufferedReader br = new BufferedReader(reader)) {
            // Read CSV
            Table df = Table.read().csv(br);
            // Convert column names to lowercase
            df.columns().forEach(c -> c.setName(c.name().toLowerCase(Locale.ROOT)));
            // Limit to nrows before filtering a few other rows - nrows is an approximate, it's ok for this example
            df = df.first(nrows);
            // Filter out the few points that are too far away from Manhattan
            Selection closeToManhattan = df.doubleColumn("lon").isBetweenInclusive(-74.03, -73.85640176685645);
            df = df.where(closeToManhattan);

            // Parse date/time column and create hour column
            StringColumn dateStrCol = (StringColumn) df.column(DATE_COLUMN);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d/yyyy H:mm:ss");
            IntColumn hourColumn = IntColumn.create("hour");

            for (int i = 0; i < dateStrCol.size(); i++) {
                String dateStr = dateStrCol.getString(i);
                LocalDateTime dt = LocalDateTime.parse(dateStr.trim(), formatter);
                hourColumn.append(dt.getHour());
            }

            // Add hour column to table
            df.addColumns(hourColumn);

            return df;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load dataset", e);
        }
    }

    private App() {
    }
}
