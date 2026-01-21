/// usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.javelit:javelit:0.78.0


import io.javelit.components.chart.EchartsComponent;
import io.javelit.core.Jt;
import org.icepear.echarts.Bar;
import org.icepear.echarts.Gauge;
import org.icepear.echarts.Line;
import org.icepear.echarts.charts.bar.BarSeries;
import org.icepear.echarts.charts.gauge.GaugeDataItem;
import org.icepear.echarts.charts.gauge.GaugeDetail;
import org.icepear.echarts.charts.gauge.GaugeProgress;
import org.icepear.echarts.charts.gauge.GaugeSeries;
import org.icepear.echarts.charts.line.LineAreaStyle;
import org.icepear.echarts.charts.line.LineSeries;
import org.icepear.echarts.components.coord.cartesian.CategoryAxis;

public class EchartsExample {
  public static void main(String[] args) throws Exception {
    Jt.title("ECharts Component Examples").use();

    Jt.text("This page demonstrates various chart types using the EchartsComponent.").use();

    // Bar Chart Example
    Jt.title("Bar Chart - Sales Data").use();
    Jt.text("A bar chart showing sales comparison between 2015 and 2016 for different products.").use();

    Bar barChart = new Bar()
        .setLegend()
        .setTooltip("item")
        .addXAxis(new String[]{"Matcha Latte", "Milk Tea", "Cheese Cocoa", "Walnut Brownie"})
        .addYAxis()
        .addSeries("2015", new Number[]{12, 83.1, 86.4, 72.4})
        .addSeries("2016", new Number[]{85.8, 73.4, 65.2, 53.9});

    Jt.echarts(barChart).height(300).theme(EchartsComponent.Theme.ROMA).use();

    // Line Chart Example
    Jt.title("Line Chart - Weekly Traffic").use();
    Jt.text("A line chart with area styling showing website traffic throughout the week.").use();

    Line lineChart = new Line()
        .addXAxis(new CategoryAxis()
                      .setData(new String[]{"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"})
                      .setBoundaryGap(false))
        .addYAxis()
        .addSeries(new LineSeries()
                       .setName("Traffic")
                       .setData(new Number[]{80, 932, 901, 934, 1290, 1330, 1320})
                       .setAreaStyle(new LineAreaStyle()));

    Jt.echarts(lineChart).use();

    Jt
        .text("💡 **Tip**: ECharts provides extensive customization options. "
              + "Check the [ECharts Java documentation](https://echarts.icepear.org) for more examples and configuration options.")
        .use();

  }
}
