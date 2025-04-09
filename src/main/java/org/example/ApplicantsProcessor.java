package org.example;

import com.google.gson.Gson;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

public class InternApp {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9.@_]*@[a-zA-Z0-9._]+[a-zA-Z]$");
    private static final Pattern SCORE_PATTERN = Pattern.compile("^\\d+(\\.\\d{1,2})?$");

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Please provide the input file path");
            return;
        }

        String inputFile = args[0];
        String outputFile = "src/main/resources/output.json";

        try {
            String jsonResult = processApplicantData(inputFile);
            Files.write(Paths.get(outputFile), jsonResult.getBytes());
            System.out.println("Rezultatul a fost salvat în " + outputFile);
        } catch (IOException e) {
            System.err.println("Error processing file: " + e.getMessage());
        }
    }

    public static String processApplicantData(String filePath) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(filePath));
        List<Applicant> applicants = new ArrayList<>();

        // Skip header if present
        int startIndex = 0;
        if (lines.size() > 0 && lines.get(0).startsWith("name,email,delivery_datetime,score")) {
            startIndex = 1;
        }

        // Parse and validate each line
        for (int i = startIndex; i < lines.size(); i++) {
            String line = lines.get(i);
            String[] parts = line.split(",");
            if (parts.length != 4) continue;

            String name = parts[0].trim();
            String email = parts[1].trim();
            String deliveryDateTimeStr = parts[2].trim();
            String scoreStr = parts[3].trim();

            try {
                if (!isValidEmail(email)) continue;

                LocalDateTime deliveryDateTime = LocalDateTime.parse(deliveryDateTimeStr, DATE_TIME_FORMATTER);
                if (!isValidScore(scoreStr)) continue;

                double score = Double.parseDouble(scoreStr);
                if (score < 0 || score > 10) continue;

                String[] nameParts = name.split("\\s+");
                if (nameParts.length < 2) continue; // Must have at least first name and last name

                String lastName = nameParts[nameParts.length - 1];
                applicants.add(new Applicant(name, email, deliveryDateTime, score, lastName));
            } catch (DateTimeParseException | NumberFormatException e) {
                continue; // Skip invalid lines
            }
        }

        // Process valid applicants
        if (applicants.isEmpty()) {
            return "{\"uniqueApplicants\": 0, \"topApplicants\": [], \"averageScore\": 0}";
        }

        // Group by email, keeping last entry for duplicates
        Map<String, Applicant> uniqueApplicantsMap = new HashMap<>();
        for (Applicant applicant : applicants) {
            uniqueApplicantsMap.put(applicant.email, applicant);
        }

        List<Applicant> uniqueApplicants = new ArrayList<>(uniqueApplicantsMap.values());
        int uniqueApplicantsCount = uniqueApplicants.size();

        // Find first and last day
        LocalDate firstDay = uniqueApplicants.stream()
                .map(a -> a.deliveryDateTime.toLocalDate())
                .min(LocalDate::compareTo)
                .orElseThrow();

        LocalDate lastDay = uniqueApplicants.stream()
                .map(a -> a.deliveryDateTime.toLocalDate())
                .max(LocalDate::compareTo)
                .orElseThrow();

        // Apply score adjustments
        for (Applicant applicant : uniqueApplicants) {
            LocalDate deliveryDate = applicant.deliveryDateTime.toLocalDate();
            LocalTime deliveryTime = applicant.deliveryDateTime.toLocalTime();

            if (!firstDay.equals(lastDay)) { // Only adjust if not all on same day
                if (deliveryDate.equals(firstDay)) {
                    applicant.adjustedScore = applicant.score + 1;
                } else if (deliveryDate.equals(lastDay) && deliveryTime.isAfter(LocalTime.of(11, 59, 59))) {
                    applicant.adjustedScore = applicant.score - 1;
                } else {
                    applicant.adjustedScore = applicant.score;
                }
            } else {
                applicant.adjustedScore = applicant.score;
            }
        }

        // Sort applicants for top 3
        uniqueApplicants.sort((a1, a2) -> {
            if (Double.compare(a2.adjustedScore, a1.adjustedScore) != 0) {
                return Double.compare(a2.adjustedScore, a1.adjustedScore);
            }
            if (Double.compare(a2.score, a1.score) != 0) {
                return Double.compare(a2.score, a1.score);
            }
            if (!a1.deliveryDateTime.equals(a2.deliveryDateTime)) {
                return a1.deliveryDateTime.compareTo(a2.deliveryDateTime);
            }
            return a1.email.compareTo(a2.email);
        });

        List<String> topApplicants = uniqueApplicants.stream()
                .limit(3)
                .map(a -> a.lastName)
                .collect(Collectors.toList());

        // Calculate average score of top half (before adjustment)
        uniqueApplicants.sort((a1, a2) -> Double.compare(a2.score, a1.score));
        int topHalfSize = (int) Math.ceil(uniqueApplicants.size() / 2.0);
        double averageScore = uniqueApplicants.stream()
                .limit(topHalfSize)
                .mapToDouble(a -> a.score)
                .average()
                .orElse(0);

        // Round to 2 decimal places (half up)
        averageScore = Math.round(averageScore * 100) / 100.0;

        // Build JSON output
        return String.format("{\"uniqueApplicants\": %d, \"topApplicants\": %s, \"averageScore\": %.2f}",
                uniqueApplicantsCount,
                new Gson().toJson(topApplicants),
                averageScore);
    }

    private static boolean isValidEmail(String email) {
        if (!EMAIL_PATTERN.matcher(email).matches()) return false;
        return email.chars().filter(c -> c == '@').count() == 1;
    }


    private static boolean isValidScore(String scoreStr) {
        return SCORE_PATTERN.matcher(scoreStr).matches();
    }

    static class Applicant {
        String name;
        String email;
        LocalDateTime deliveryDateTime;
        double score;
        String lastName;
        double adjustedScore;

        public Applicant(String name, String email, LocalDateTime deliveryDateTime, double score, String lastName) {
            this.name = name;
            this.email = email;
            this.deliveryDateTime = deliveryDateTime;
            this.score = score;
            this.lastName = lastName;
            this.adjustedScore = score;
        }
    }
}
