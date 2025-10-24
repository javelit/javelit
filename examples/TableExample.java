///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.javelit:javelit:0.56.0

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.javelit.core.Jt;

public class TableExample {
    public record Info(int age, String size) {
    }

    public record Person(String name, Info info) {
    }

    public static void main(String[] args) throws Exception {
        Jt.title("Table Component Examples").use();

        Jt.text("This page demonstrates the TableComponent with various data formats.").use();

        // Example 1: Simple table from Map<>
        Jt.title("Basic Table - Sales Data").use();
        Jt.text("From a List of records").use();

        final List<Object> rows = List.of(
                new Person("Cyril", new Info(21, "big")),
                new Person("Paul", new Info(23, "very big"))
        );
        Jt.table(rows).use();


        Jt.text("From a Map of columns (ColumnName -> ColumnValues) ").use();

        final Map<String, List<Object>> columns = new LinkedHashMap<>();
        columns.put("Name", List.of("Sun wars", "The lord of the wings"));
        var ratings = new ArrayList<>();
        ratings.add(1);
        ratings.add(null);
        columns.put("Rating", ratings);
        Jt.tableFromListColumns(columns).use();
    }
}
