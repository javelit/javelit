 import io.javelit.core.Jt;
 import org.icepear.echarts.Option;
 import org.icepear.echarts.charts.bar.BarSeries;
 import org.icepear.echarts.components.coord.cartesian.CategoryAxis;
 import org.icepear.echarts.components.coord.cartesian.ValueAxis;
 import org.icepear.echarts.origin.util.SeriesOption;

 public class OptionChartApp {
     public static void main(String[] args) {
         CategoryAxis xAxis = new CategoryAxis()
                 .setType("category")
                 .setData(new String[]{"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"});

         ValueAxis yAxis = new ValueAxis().setType("value");

         BarSeries series = new BarSeries()
                 .setData(new Number[]{120, 200, 150, 80, 70, 110, 130})
                 .setType("bar");

         Option option = new Option()
                 .setXAxis(xAxis)
                 .setYAxis(yAxis)
                 .setSeries(new SeriesOption[]{series});

         Jt.echarts(option).use();
     }
 }
