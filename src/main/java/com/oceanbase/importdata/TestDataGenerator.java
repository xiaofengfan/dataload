package com.oceanbase.importdata;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Random;

public class TestDataGenerator {

    private static final String[] FIRST_NAMES = {"zhang", "li", "wang", "liu", "chen", "yang", "huang", "zhao", "zhou", "wu", "xu", "sun", "ma", "zhu", "hu", "guo", "lin", "he", "gao", "luo"};
    private static final String[] LAST_NAMES = {"wei", "fang", "na", "xiuying", "min", "jing", "li", "qiang", "lei", "jun", "yang", "yong", "yan", "jie", "tao", "ming", "chao", "xiulan", "hai", "xia"};
    private static final String[] CITIES = {"beijing", "shanghai", "shenzhen", "guangzhou", "hangzhou", "chengdu", "wuhan", "xian", "nanjing", "chongqing", "tianjin", "suzhou", "dalian", "qingdao", "ningbo", "xiamen", "changsha", "zhengzhou", "shenyang", "jinan"};
    private static final String[] PRODUCTS = {"phone", "computer", "tablet", "tv", "fridge", "washer", "ac", "car", "bicycle", "book", "clothes", "shoes", "watch", "glasses", "bag"};
    private static final String[] STATUS = {"normal", "error", "pending", "completed", "cancelled", "processing", "paused", "review"};

    private static final Random random = new Random();

    public static void main(String[] args) throws Exception {
        String outputPath = args.length > 0 ? args[0] : "./test_data.csv";
        int totalRows = args.length > 1 ? Integer.parseInt(args[1]) : 50000;
        int batchSize = 10000;

        System.out.println("Starting test data generation...");
        System.out.println("Output: " + outputPath);
        System.out.println("Total rows: " + totalRows);
        System.out.println("Batch size: " + batchSize);

        File file = new File(outputPath);
        if (file.exists()) {
            file.delete();
        }

        long startTime = System.currentTimeMillis();

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(
                        new FileOutputStream(file),
                        StandardCharsets.UTF_8))) {

            writer.write("id,col_text_01,col_text_02,col_text_03,col_text_04,col_text_05,col_number_01,col_number_02,col_number_03,col_number_04,col_number_05,col_number_06,col_number_07,col_number_08,col_number_09,col_number_10,col_varchar_01,col_varchar_02,col_varchar_03,col_varchar_04,col_varchar_05\n");

            for (int i = 0; i < totalRows; i++) {
                long rowNum = i + 1;
                String row = generateRow(rowNum);
                writer.write(row);
                writer.write("\n");

                if ((i + 1) % batchSize == 0) {
                    writer.flush();
                    long elapsed = System.currentTimeMillis() - startTime;
                    double progress = ((i + 1) * 100.0) / totalRows;
                    double speed = (i + 1) * 1000.0 / elapsed;
                    long remaining = (long) ((totalRows - i - 1) / speed);
                    System.out.printf("\rProgress: %.2f%% | Rows: %,d / %,d | Speed: %,.0f rows/s | ETA: %ds",
                            progress, i + 1, totalRows, speed, remaining);
                }
            }
            writer.flush();
        }

        long totalTime = System.currentTimeMillis() - startTime;
        File generatedFile = new File(outputPath);
        long fileSizeKB = generatedFile.length() / 1024;

        System.out.println("\n");
        System.out.println("============================================");
        System.out.println("Generation completed!");
        System.out.println("File: " + generatedFile.getAbsolutePath());
        System.out.println("Size: " + fileSizeKB + " KB");
        System.out.println("Rows: " + totalRows + " (excluding header)");
        System.out.println("Time: " + (totalTime / 1000) + " seconds");
        System.out.println("Speed: " + String.format("%,.0f", totalRows * 1000.0 / totalTime) + " rows/s");
        System.out.println("============================================");
    }

    private static String generateRow(long rowNum) {
        StringBuilder sb = new StringBuilder();

        sb.append(rowNum).append(",");
        sb.append(generateText(rowNum, 80)).append(",");
        sb.append(generateText(rowNum, 100)).append(",");
        sb.append(generateText(rowNum, 120)).append(",");
        sb.append(generateText(rowNum, 90)).append(",");
        sb.append(generateText(rowNum, 110)).append(",");

        sb.append(String.format("%.4f,", random.nextDouble() * 1000000));
        sb.append(String.format("%.4f,", random.nextDouble() * 500000));
        sb.append(String.format("%.4f,", random.nextDouble() * 100000));
        sb.append(random.nextInt(10000)).append(",");
        sb.append(random.nextInt(1000)).append(",");
        sb.append(String.format("%.4f,", random.nextDouble() * 99999));
        sb.append(random.nextInt(100)).append(",");
        sb.append(random.nextInt(1000000000)).append(",");
        sb.append(String.format("%.4f,", random.nextDouble() * 1000));
        sb.append(random.nextInt(100000)).append(",");

        sb.append(generateMd5(String.valueOf(rowNum))).append(",");
        sb.append(randomFromArray(FIRST_NAMES)).append(randomFromArray(LAST_NAMES)).append(",");
        sb.append(randomFromArray(CITIES)).append(random.nextInt(1000)).append(",");
        sb.append(randomFromArray(PRODUCTS)).append("-").append(random.nextInt(10000)).append(",");
        sb.append(randomFromArray(STATUS));

        return sb.toString();
    }

    private static String generateText(long seed, int maxLen) {
        String name1 = randomFromArray(FIRST_NAMES);
        String name2 = randomFromArray(LAST_NAMES);
        String city = randomFromArray(CITIES);
        String product = randomFromArray(PRODUCTS);
        String status = randomFromArray(STATUS);
        double amount = random.nextDouble() * 10000;
        int quantity = random.nextInt(100) + 1;

        String text = String.format("order:%d user:%s%s city:%s product:%s status:%s amount:%.2f qty:%d",
                seed, name1, name2, city, product, status, amount, quantity);

        if (text.length() > maxLen) {
            text = text.substring(0, maxLen);
        }

        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    private static String generateMd5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.substring(0, 24);
        } catch (Exception e) {
            return String.valueOf(input.hashCode());
        }
    }

    private static String randomFromArray(String[] array) {
        return array[random.nextInt(array.length)];
    }
}
