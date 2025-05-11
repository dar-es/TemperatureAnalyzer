package com.example.tempanalyzer;

import com.opencsv.CSVReader;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class TemperatureAnalyzer {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String CSV_PATH = "Temperaturas.csv";

    public static void main(String[] args) {
        List<Record> records = loadData(CSV_PATH);
        Scanner scanner = new Scanner(System.in);

        System.out.println("Ingrese fecha inicial (dd/MM/yyyy):");
        LocalDate start = LocalDate.parse(scanner.nextLine(), FORMATTER);
        System.out.println("Ingrese fecha final (dd/MM/yyyy):");
        LocalDate end = LocalDate.parse(scanner.nextLine(), FORMATTER);

        Map<String, Double> avgTemps = records.stream()
            .filter(r -> !r.date.isBefore(start) && !r.date.isAfter(end))
            .collect(Collectors.groupingBy(
                r -> r.city,
                Collectors.averagingDouble(r -> r.temperature)
            ));

        generateBarChart(avgTemps, start, end);

        System.out.println("\nIngrese fecha para consulta puntual (dd/MM/yyyy):");
        LocalDate query = LocalDate.parse(scanner.nextLine(), FORMATTER);

        List<Record> onDate = records.stream()
            .filter(r -> r.date.equals(query))
            .collect(Collectors.toList());
        if (onDate.isEmpty()) {
            System.out.println("No hay datos para la fecha especificada.");
        } else {
            Record hottest = onDate.stream().max(Comparator.comparingDouble(r -> r.temperature)).get();
            Record coldest = onDate.stream().min(Comparator.comparingDouble(r -> r.temperature)).get();
            System.out.printf("Fecha %s: Ciudad más calurosa: %s (%.1f°C)\n", query.format(FORMATTER), hottest.city, hottest.temperature);
            System.out.printf("Fecha %s: Ciudad más fría: %s (%.1f°C)\n", query.format(FORMATTER), coldest.city, coldest.temperature);
        }

        scanner.close();
    }

    private static List<Record> loadData(String path) {
        List<Record> list = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new FileReader(path))) {
            String[] line;
            while ((line = reader.readNext()) != null) {
                if (line.length < 3) continue;
                String city = line[0].trim();
                LocalDate date = LocalDate.parse(line[1].trim(), FORMATTER);
                double temp = Double.parseDouble(line[2].trim());
                list.add(new Record(city, date, temp));
            }
        } catch (IOException e) {
            System.err.println("Error leyendo el CSV: " + e.getMessage());
        }
        return list;
    }

    private static void generateBarChart(Map<String, Double> data, LocalDate start, LocalDate end) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        data.forEach((city, avg) -> dataset.addValue(avg, "Temperatura Promedio", city));

        String title = String.format("Temperatura promedio por ciudad\n%s - %s",
            start.format(FORMATTER), end.format(FORMATTER));
        JFreeChart barChart = ChartFactory.createBarChart(
            title,
            "Ciudad",
            "°C",
            dataset
        );

        try {
            String filename = String.format("avg_temps_%s_to_%s.png", start, end);
            ChartUtils.saveChartAsPNG(Paths.get(filename).toFile(), barChart, 800, 600);
            System.out.println("Gráfica guardada en: " + filename);
        } catch (IOException e) {
            System.err.println("Error al guardar la gráfica: " + e.getMessage());
        }
    }

    private static class Record {
        String city;
        LocalDate date;
        double temperature;

        Record(String city, LocalDate date, double temperature) {
            this.city = city;
            this.date = date;
            this.temperature = temperature;
        }
    }
}
