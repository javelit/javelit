 import io.javelit.core.Jt;
 import org.icepear.echarts.Bar;

 public class BarChartApp {
     public static void main(String[] args) {
         Bar bar = new Bar()
                 .setLegend()
                 .setTooltip("item")
                 .addXAxis(new String[]{"Matcha Latte", "Milk Tea", "Cheese Cocoa", "Walnut Brownie"})
                 .addYAxis()
                 .addSeries("2015", new Number[]{43.3, 83.1, 86.4, 72.4})
                 .addSeries("2016", new Number[]{85.8, 73.4, 65.2, 53.9})
                 .addSeries("2017", new Number[]{93.7, 55.1, 82.5, 39.1});

         Jt.echarts(bar).use();
     }
 }
