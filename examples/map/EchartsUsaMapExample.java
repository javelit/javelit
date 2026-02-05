/// usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS io.javelit:javelit:0.85.0
// NOTE: this example is a work in progress - the echarts-java project support for maps is limited at the moment
package map;

import java.net.URI;
import java.util.Map;

import io.javelit.components.chart.EchartsComponent;
import io.javelit.core.Jt;
import org.icepear.echarts.Option;
import org.icepear.echarts.charts.scatter.ScatterSeries;
import org.icepear.echarts.components.series.ItemStyle;
import org.icepear.echarts.components.title.Title;
import org.icepear.echarts.components.toolbox.Toolbox;
import org.icepear.echarts.components.toolbox.ToolboxDataViewFeature;
import org.icepear.echarts.components.toolbox.ToolboxRestoreFeature;
import org.icepear.echarts.components.toolbox.ToolboxSaveAsImageFeature;
import org.icepear.echarts.components.tooltip.Tooltip;
import org.icepear.echarts.components.visualMap.ContinousVisualMap;
import org.icepear.echarts.origin.component.marker.MarkAreaOption;
import org.icepear.echarts.origin.component.marker.MarkLineOption;
import org.icepear.echarts.origin.component.marker.MarkPointOption;
import org.icepear.echarts.origin.export.SeriesInjectedOption;
import org.icepear.echarts.origin.util.AnimationOptionMixin;
import org.icepear.echarts.origin.util.ColorPaletteOptionMixin;
import org.icepear.echarts.origin.util.ComponentOption;
import org.icepear.echarts.origin.util.LabelLayoutOption;
import org.icepear.echarts.origin.util.LabelLineOption;
import org.icepear.echarts.origin.util.SeriesOption;
import org.icepear.echarts.origin.util.StatesOptionMixin;
import org.icepear.echarts.serializer.EChartsSerializer;

public class EchartsUsaMapExample {

  public static void main(String[] args) {
    Jt.title("USA Population Estimates (2012)").use();
    Jt.text("This example demonstrates ECharts map functionality with special area positioning").use();

    // Define special areas for Alaska, Hawaii, and Puerto Rico
    Map<String, EchartsComponent.SpecialAreaConfig> specialAreas = Map.of("Alaska",
                                                                          new EchartsComponent.SpecialAreaConfig(-131,
                                                                                                                 25,
                                                                                                                 15),
                                                                          "Hawaii",
                                                                          new EchartsComponent.SpecialAreaConfig(-110,
                                                                                                                 28,
                                                                                                                 5),
                                                                          "Puerto Rico",
                                                                          new EchartsComponent.SpecialAreaConfig(-76,
                                                                                                                 26,
                                                                                                                 2));

    // Create the population data
    Object[] populationData = new Object[]{Map.of("name", "Alabama", "value", 4822023), Map.of("name",
                                                                                               "Alaska",
                                                                                               "value",
                                                                                               731449), Map.of(
        "name",
        "Arizona",
        "value",
        6553255), Map.of("name", "Arkansas", "value", 2949131), Map.of("name",
                                                                       "California",
                                                                       "value",
                                                                       38041430), Map.of("name",
                                                                                         "Colorado",
                                                                                         "value",
                                                                                         5187582), Map.of("name",
                                                                                                          "Connecticut",
                                                                                                          "value",
                                                                                                          3590347), Map.of(
        "name",
        "Delaware",
        "value",
        917092), Map.of("name", "District of Columbia", "value", 632323), Map.of("name",
                                                                                 "Florida",
                                                                                 "value",
                                                                                 19317568), Map.of("name",
                                                                                                   "Georgia",
                                                                                                   "value",
                                                                                                   9919945), Map.of(
        "name",
        "Hawaii",
        "value",
        1392313), Map.of("name", "Idaho", "value", 1595728), Map.of("name",
                                                                    "Illinois",
                                                                    "value",
                                                                    12875255), Map.of("name",
                                                                                      "Indiana",
                                                                                      "value",
                                                                                      6537334), Map.of("name",
                                                                                                       "Iowa",
                                                                                                       "value",
                                                                                                       3074186), Map.of(
        "name",
        "Kansas",
        "value",
        2885905), Map.of("name", "Kentucky", "value", 4380415), Map.of("name",
                                                                       "Louisiana",
                                                                       "value",
                                                                       4601893), Map.of("name",
                                                                                        "Maine",
                                                                                        "value",
                                                                                        1329192), Map.of("name",
                                                                                                         "Maryland",
                                                                                                         "value",
                                                                                                         5884563), Map.of(
        "name",
        "Massachusetts",
        "value",
        6646144), Map.of("name", "Michigan", "value", 9883360), Map.of("name",
                                                                       "Minnesota",
                                                                       "value",
                                                                       5379139), Map.of("name",
                                                                                        "Mississippi",
                                                                                        "value",
                                                                                        2984926), Map.of("name",
                                                                                                         "Missouri",
                                                                                                         "value",
                                                                                                         6021988), Map.of(
        "name",
        "Montana",
        "value",
        1005141), Map.of("name", "Nebraska", "value", 1855525), Map.of("name",
                                                                       "Nevada",
                                                                       "value",
                                                                       2758931), Map.of("name",
                                                                                        "New Hampshire",
                                                                                        "value",
                                                                                        1320718), Map.of("name",
                                                                                                         "New Jersey",
                                                                                                         "value",
                                                                                                         8864590), Map.of(
        "name",
        "New Mexico",
        "value",
        2085538), Map.of("name", "New York", "value", 19570261), Map.of("name",
                                                                        "North Carolina",
                                                                        "value",
                                                                        9752073), Map.of("name",
                                                                                         "North Dakota",
                                                                                         "value",
                                                                                         699628), Map.of("name",
                                                                                                         "Ohio",
                                                                                                         "value",
                                                                                                         11544225), Map.of(
        "name",
        "Oklahoma",
        "value",
        3814820), Map.of("name", "Oregon", "value", 3899353), Map.of("name",
                                                                     "Pennsylvania",
                                                                     "value",
                                                                     12763536), Map.of("name",
                                                                                       "Rhode Island",
                                                                                       "value",
                                                                                       1050292), Map.of("name",
                                                                                                        "South Carolina",
                                                                                                        "value",
                                                                                                        4723723), Map.of(
        "name",
        "South Dakota",
        "value",
        833354), Map.of("name", "Tennessee", "value", 6456243), Map.of("name",
                                                                       "Texas",
                                                                       "value",
                                                                       26059203), Map.of("name",
                                                                                         "Utah",
                                                                                         "value",
                                                                                         2855287), Map.of("name",
                                                                                                          "Vermont",
                                                                                                          "value",
                                                                                                          626011), Map.of(
        "name",
        "Virginia",
        "value",
        8185867), Map.of("name", "Washington", "value", 6897012), Map.of("name",
                                                                         "West Virginia",
                                                                         "value",
                                                                         1855413), Map.of("name",
                                                                                          "Wisconsin",
                                                                                          "value",
                                                                                          5726398), Map.of(
        "name",
        "Wyoming",
        "value",
        576412), Map.of("name", "Puerto Rico", "value", 3667084)};

    // Build the ECharts option
    Option option = new Option()
        .setTitle(new Title()
                      .setText("USA Population Estimates (2012)")
                      .setSubtext("Data from www.census.gov")
                      .setSublink("http://www.census.gov/popest/data/datasets.html")
                      .setLeft("right"))
        .setTooltip(new Tooltip().setTrigger("item").setShowDelay(0).setTransitionDuration(0.2))
        .setVisualMap(new ContinousVisualMap()
                          .setLeft("right")
                          .setMin(500000)
                          .setMax(38000000)
                          .setColor(new String[]{"#a50026", "#d73027", "#f46d43", "#fdae61", "#fee090", "#ffffbf", "#e0f3f8", "#abd9e9", "#74add1", "#4575b4", "#313695"})
                          .setText(new String[]{"High", "Low"})
                          .setCalculable(true))
        .setToolbox(new Toolbox()
                        .setShow(true)
                        .setLeft("left")
                        .setTop("top")
                        .setFeature(Map.of("dataView",
                                           new ToolboxDataViewFeature().setReadOnly(false),
                                           "restore",
                                           new ToolboxRestoreFeature(),
                                           "saveAsImage",
                                           new ToolboxSaveAsImageFeature())))
        .setSeries(new SeriesOption[]{new MapSeriesOption(populationData)});

    // Create the chart with the USA map
    Jt.echarts(option).withMap("USA", URI.create("/app/static/usa.json"), specialAreas).height(600).use();

    Jt.markdown("---").use();
    Jt.text("The map shows population data from 2012 US Census.").use();
    Jt.text("Special areas like Alaska, Hawaii, and Puerto Rico are repositioned for better visualization.").use();

    Jt.markdown("## Another map").use();

    Object[] data = new Object[]{Map.of("name",
                                        "UBER",
                                        "value",
                                        new Double[]{-73.98453916661742, 40.729735717835275}), Map.of("name",
                                                                                                      "UBER",
                                                                                                      "value",
                                                                                                      new Double[]{40.729735717835275, -73.98453916661742}), Map.of(
        "name",
        "2",
        "value",
        new Double[]{-73.98453916661742, 40.730735717835275})};
    Option option2 = new Option()
        .setGeo(Map.of("map",
                       "manhattan",
                       "roam",
                       true,
                       "zoom",
                       1.2,
                       "scaleLimit",
                       Map.of("min", 0.8, "max", 8),
                       "tooltip",
                       Map.of("show", true)))
        .setSeries(new ScatterSeries()
                       .setData(data)
                       .setCoordinateSystem("geo")
                       .setSymbolSize(13)
                       .setItemStyle(new ItemStyle().setColor("#b02a02").setOpacity(0.5)));

    System.out.println(new EChartsSerializer().toJson(option2));

    Jt.echarts(option2).withMap("manhattan", URI.create("/app/static/manhattan.geo.json")).height(700).use();
  }

  public static class MapSeriesOption implements SeriesOption {
    private String type;

    private String name;
    private boolean roam;
    private String map;
    private Map<String, Object> emphasis;
    private Object data;

    public MapSeriesOption(Object[] populationData) {
      type = "map";
      name = "USA PopEstimates";
      roam = true;
      map = "USA";
      emphasis = Map.of("label", Map.of("show", true));
      data = populationData;
    }

    @Override
    public SeriesOption setMainType(String mainType) {
      return null;
    }

    @Override
    public SeriesOption setSilent(Boolean silent) {
      return null;
    }

    @Override
    public SeriesOption setBlendMode(String blendMode) {
      return null;
    }

    @Override
    public SeriesOption setCursor(String cursor) {
      return null;
    }

    @Override
    public SeriesOption setDataGroupId(Number dataGroupId) {
      return null;
    }

    @Override
    public SeriesOption setDataGroupId(String dataGroupId) {
      return null;
    }

    @Override
    public SeriesOption setData(Object data) {
      return null;
    }

    @Override
    public SeriesOption setColorBy(String colorBy) {
      return null;
    }

    @Override
    public SeriesOption setLegendHoverLink(Boolean legendHoverLink) {
      return null;
    }

    @Override
    public SeriesOption setProgressive(Boolean progressive) {
      return null;
    }

    @Override
    public SeriesOption setProgressive(Number progressive) {
      return null;
    }

    @Override
    public SeriesOption setProgressiveThreshold(Number progressiveThreshold) {
      return null;
    }

    @Override
    public SeriesOption setProgressiveChunkMode(String progressiveChunkMode) {
      return null;
    }

    @Override
    public SeriesOption setCoordinateSystem(String coordinateSystem) {
      return null;
    }

    @Override
    public SeriesOption setHoverLayerThreshold(Number hoverLayerThreshold) {
      return null;
    }

    @Override
    public SeriesOption setSeriesLayoutBy(String seriesLayoutBy) {
      return null;
    }

    @Override
    public SeriesOption setLabelLine(LabelLineOption labelLine) {
      return null;
    }

    @Override
    public SeriesOption setLabelLayout(LabelLayoutOption labelLayout) {
      return null;
    }

    @Override
    public SeriesOption setStateAnimation(Object stateAnimation) {
      return null;
    }

    @Override
    public SeriesOption setUniversalTransition(Boolean universalTransition) {
      return null;
    }

    @Override
    public SeriesOption setUniversalTransition(Object universalTransition) {
      return null;
    }

    @Override
    public SeriesOption setSelectedMap(Map<String, Boolean> selectedMap) {
      return null;
    }

    @Override
    public SeriesOption setSelectedMode(Boolean selectedMode) {
      return null;
    }

    @Override
    public SeriesOption setSelectedMode(String selectedMode) {
      return null;
    }

    @Override
    public SeriesInjectedOption setMarkArea(MarkAreaOption markArea) {
      return null;
    }

    @Override
    public SeriesInjectedOption setMarkLine(MarkLineOption markLine) {
      return null;
    }

    @Override
    public SeriesInjectedOption setMarkPoint(MarkPointOption markPoint) {
      return null;
    }

    @Override
    public SeriesInjectedOption setTooltip(Object tooltip) {
      return null;
    }

    @Override
    public AnimationOptionMixin setAnimation(Boolean animation) {
      return null;
    }

    @Override
    public AnimationOptionMixin setAnimationThreshold(Number animationThreshold) {
      return null;
    }

    @Override
    public AnimationOptionMixin setAnimationDuration(Number animationDuration) {
      return null;
    }

    @Override
    public AnimationOptionMixin setAnimationDuration(Object animationDuration) {
      return null;
    }

    @Override
    public AnimationOptionMixin setAnimationEasing(Object animationEasing) {
      return null;
    }

    @Override
    public AnimationOptionMixin setAnimationDelay(Number animationDelay) {
      return null;
    }

    @Override
    public AnimationOptionMixin setAnimationDelay(Object animationDelay) {
      return null;
    }

    @Override
    public AnimationOptionMixin setAnimationDurationUpdate(Number animationDurationUpdate) {
      return null;
    }

    @Override
    public AnimationOptionMixin setAnimationDurationUpdate(Object animationDurationUpdate) {
      return null;
    }

    @Override
    public AnimationOptionMixin setAnimationEasingUpdate(Object animationEasingUpdate) {
      return null;
    }

    @Override
    public AnimationOptionMixin setAnimationDelayUpdate(Number animationDelayUpdate) {
      return null;
    }

    @Override
    public AnimationOptionMixin setAnimationDelayUpdate(Object animationDelayUpdate) {
      return null;
    }

    @Override
    public ColorPaletteOptionMixin setColor(String color) {
      return null;
    }

    @Override
    public ColorPaletteOptionMixin setColor(String[] color) {
      return null;
    }

    @Override
    public ColorPaletteOptionMixin setColorLayer(String[][] colorLayer) {
      return null;
    }

    @Override
    public ComponentOption setType(String type) {
      return null;
    }

    @Override
    public ComponentOption setId(Number id) {
      return null;
    }

    @Override
    public ComponentOption setId(String id) {
      return null;
    }

    @Override
    public ComponentOption setName(Number name) {
      return null;
    }

    @Override
    public ComponentOption setName(String name) {
      return null;
    }

    @Override
    public ComponentOption setZ(Number z) {
      return null;
    }

    @Override
    public ComponentOption setZlevel(Number zlevel) {
      return null;
    }

    @Override
    public StatesOptionMixin setEmphasis(Object emphasis) {
      return null;
    }

    @Override
    public StatesOptionMixin setSelect(Object select) {
      return null;
    }

    @Override
    public StatesOptionMixin setBlur(Object blur) {
      return null;
    }
  }
}
