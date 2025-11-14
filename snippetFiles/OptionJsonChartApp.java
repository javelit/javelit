import io.javelit.core.Jt;
import org.icepear.echarts.Option;
import org.icepear.echarts.charts.bar.BarSeries;
import org.icepear.echarts.components.coord.cartesian.CategoryAxis;
import org.icepear.echarts.components.coord.cartesian.ValueAxis;
import org.icepear.echarts.origin.util.SeriesOption;

public class OptionJsonChartApp {
  public static void main(String[] args) {
    String echartsOptionJson = """
        {
          "xAxis": {
            "type": "category",
            "data": ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"]
          },
          "yAxis": {
            "type": "value"
          },
          "series": [
            {
              "data": [150, 230, 224, 218, 135, 147, 260],
              "type": "line"
            }
          ]
        }
        """;

    Jt.echarts(echartsOptionJson).use();
  }
}
