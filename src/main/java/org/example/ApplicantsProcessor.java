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

public class ApplicantsProcessor {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

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

        int startIndex = 0;

        // Skipping header line
        if (lines.size() > 0 && lines.get(0).startsWith("name,email,delivery_datetime,score")) {
            startIndex = 1;
        }

        for (int i = startIndex; i < lines.size(); i++) {
            String line = lines.get(i);
            String[] parts = line.split(",");
            if (parts.length != 4) continue;

            String name = parts[0].trim();
            String email = parts[1].trim();
            String deliveryDateTimeStr = parts[2].trim();
            String scoreStr = parts[3].trim();

            // Validating the data in current line
            // If the data is not the correct format the line will be skipped
            try {
                if (!isValidEmail(email)) continue;

                LocalDateTime deliveryDateTime = LocalDateTime.parse(deliveryDateTimeStr, DATE_TIME_FORMATTER);
                if (!isValidScore(scoreStr)) continue;

                double score = Double.parseDouble(scoreStr);
                if (score < 0 || score > 10) continue;

                String[] nameParts = name.split("\\s+");
                if (nameParts.length < 2) continue;

                applicants.add(new Applicant(name, email, deliveryDateTime, score));
            } catch (DateTimeParseException | NumberFormatException e) {
                continue;
            }
        }

        if (applicants.isEmpty()) {
            return "{\"uniqueApplicants\": 0, \"topApplicants\": [], \"averageScore\": 0}";
        }

        /// Removing repeating email lines
        // Adding the correct applicant in a Map using the email as a key
        Map<String, Applicant> uniqueApplicantsMap = new HashMap<>();
        for (Applicant applicant : applicants) {
            uniqueApplicantsMap.put(applicant.getEmail(), applicant);
        }

        // Adding the applicants without repeating emails back in a list
        List<Applicant> uniqueApplicants = new ArrayList<>(uniqueApplicantsMap.values());
        int uniqueApplicantsCount = uniqueApplicants.size();

        /// Making the bonus/penalty system
        // saving the date of the first day when someone sent the solution using java streams
        LocalDate firstDay = uniqueApplicants.stream()
                .map(a -> a.getDeliveryDateTime().toLocalDate())
                .min(LocalDate::compareTo)
                .orElseThrow();

        // saving the date of the last day when someone sent the solution
        LocalDate lastDay = uniqueApplicants.stream()
                .map(a -> a.getDeliveryDateTime().toLocalDate())
                .max(LocalDate::compareTo)
                .orElseThrow();

        // Applying bonus/penalty point on the score based on the day the solution was sent
        for (Applicant applicant : uniqueApplicants) {
            LocalDate deliveryDate = applicant.getDeliveryDateTime().toLocalDate();
            LocalTime deliveryTime = applicant.getDeliveryDateTime().toLocalTime();

            if (!firstDay.equals(lastDay)) {
                if (deliveryDate.equals(firstDay)) {
                    applicant.setAdjustedScore(applicant.getScore() +1); // 1 extra point for sending in the first day
                } else if (deliveryDate.equals(lastDay) && deliveryTime.isAfter(LocalTime.of(11, 59, 59))) {
                    applicant.setAdjustedScore(applicant.getScore() -1); // 1 point penalty for sending in the second half of the last day
                }
            }
        }

        /// Getting the top 3
        // Sorting the applicants for top 3
        uniqueApplicants.sort((a1, a2) -> {
            if (Double.compare(a2.getAdjustedScore(), a1.getAdjustedScore()) != 0) {
                return Double.compare(a2.getAdjustedScore(), a1.getAdjustedScore());
            }
            if (Double.compare(a2.getScore(), a1.getScore()) != 0) {
                return Double.compare(a2.getScore(), a1.getScore());
            }
            if (!a1.getDeliveryDateTime().equals(a2.getDeliveryDateTime())) {
                return a1.getDeliveryDateTime().compareTo(a2.getDeliveryDateTime());
            }
            return a1.getEmail().compareTo(a2.getEmail());
        });

        // Adding the last name of each applicant in the top 3 to a list
        List<String> topApplicants = uniqueApplicants.stream()
                .limit(3)
                .map(Applicant::getLastName)
                .collect(Collectors.toList());

        /// Calculating the average score of top half before adjustment
        uniqueApplicants.sort((a1, a2) -> Double.compare(a2.getScore(), a1.getScore()));

        int topHalfSize = (int) Math.ceil(uniqueApplicants.size() / 2.0); // if we have an odd number size the .5 result gets rounded up

        double averageScore = uniqueApplicants.stream()
                .limit(topHalfSize)
                .mapToDouble(Applicant::getScore)
                .average()
                .orElse(0);

        // Rounding the score to 2 decimals
        averageScore = Math.round(averageScore * 100) / 100.0;

        /// Building the json output
        return String.format("{\"uniqueApplicants\": %d, \"topApplicants\": %s, \"averageScore\": %.2f}",
                uniqueApplicantsCount,
                new Gson().toJson(topApplicants),
                averageScore);
    }

    /// Validating the data ----------------------------------------------------------------------------------------------------------------

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9.@_]*@[a-zA-Z0-9._]+[a-zA-Z]$");
    private static final Pattern SCORE_PATTERN = Pattern.compile("^\\d+(\\.\\d{1,2})?$");

    private static boolean isValidEmail(String email) {
        if (!EMAIL_PATTERN.matcher(email).matches()) return false;
        return email.chars().filter(c -> c == '@').count() == 1;
    }

    private static boolean isValidScore(String scoreStr) {
        return SCORE_PATTERN.matcher(scoreStr).matches();
    }

}
