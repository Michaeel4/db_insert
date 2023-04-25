import me.tongfei.progressbar.ProgressBar;
import org.knowm.xchart.*;

import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.CategoryChartBuilder;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws SQLException, IOException {
        String url = "jdbc:postgresql://localhost/tuning";
        String user = "michael045";
        String password = "";
        String filePath = "/Users/michael045/Desktop/Datenbanken2/auth.tsv";

        System.out.println("Please select an algorithm to use:");
        System.out.println("1. Straightforward");
        System.out.println("2. Batch Insert");
        System.out.println("3. Copy Command");
        System.out.println("4. Run all algorithms");

g
        double straightforwardTime = 0;
        double batchInsertTime = 0;
        double loadWithCopyTime = 0;

        Scanner scanner = new Scanner(System.in);
        int algorithm = scanner.nextInt();

        switch (algorithm) {
            case 1:
                System.out.println("Using Straightforward Algorithm");
                straightforwardTime = loadWithStraightforward(url, user, password, filePath);
                break;
            case 2:
                System.out.println("Using Batch Insert Algorithm");
                batchInsertTime = loadWithBatchInsert(url, user, password, filePath);
                break;
            case 3:
                System.out.println("Using Copy Command Algorithm");
                loadWithCopyTime = loadWithCopyCommand(url, user, password, filePath);
                break;

            case 4:
                System.out.println("Running all algorithms");

                System.out.println("Running Straightforward Algorithm");
                straightforwardTime = loadWithStraightforward(url, user, password, filePath);

                System.out.println("Running Batch Insert Algorithm");
                batchInsertTime = loadWithBatchInsert(url, user, password, filePath);

                System.out.println("Running Copy Command Algorithm");
                loadWithCopyTime = loadWithCopyCommand(url, user, password, filePath);
                break;
            default:
                System.out.println("Invalid selection");

        }




        String[] algorithms = {"Straightforward", "Batch Insert", "Copy Command"};
        double[] loadingTimes = {straightforwardTime, batchInsertTime, loadWithCopyTime};

        CategoryChart chart = new CategoryChartBuilder()
                .title("Loading Time Comparison")
                .xAxisTitle("Algorithm")
                .yAxisTitle("Loading Time (ms)")
                .build();


        List<String> xData = Arrays.asList("Straightforward", "Batch Insert", "Copy Command");
        List<Double> yData = Arrays.asList(straightforwardTime, batchInsertTime, loadWithCopyTime);
        chart.addSeries("Loading Time", xData, yData);

        BitmapEncoder.saveBitmap(chart, "./loading_time_comparison.png", BitmapEncoder.BitmapFormat.PNG);

    }

    public static long loadWithStraightforward(String url, String user, String password, String filePath) {
        long startTime = System.currentTimeMillis();

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement();
             BufferedReader br = new BufferedReader(new FileReader(filePath))) {

            String sql = "CREATE TABLE IF NOT EXISTS auth " +
                    "(id SERIAL not NULL, " +
                    " name VARCHAR(49), " +
                    " pubID VARCHAR(129)) ";

            stmt.executeUpdate(sql);

            String line;

            // Get the total number of lines in the file
            long totalLines = Files.lines(Paths.get(filePath)).count();

            // Initialize the progress bar
            ProgressBar progressBar = new ProgressBar("Progress", totalLines);

            while ((line = br.readLine()) != null) {
                String[] values = line.split("\t");
                String name = values[0];
                String pubID = values[1];

                String query = "INSERT INTO auth (name, pubID) VALUES (?, ?)";
                PreparedStatement pstmt = conn.prepareStatement(query);
                pstmt.setString(1, name);
                pstmt.setString(2, pubID);
                pstmt.executeUpdate();
                pstmt.close();

                progressBar.step();
                progressBar.setExtraMessage(String.format("%.2f%%", (double)progressBar.getCurrent() * 100 / totalLines));
            }

            System.out.println("Data imported successfully");

        } catch (Exception e) {
            e.printStackTrace();
        }

        long endTime = System.currentTimeMillis();
        long timeTaken = endTime - startTime;
        System.out.println("Time taken: " + timeTaken + " milliseconds");
        return timeTaken;
    }

    public static long loadWithCopyCommand(String url, String user, String password, String filePath) {
        long startTime = System.currentTimeMillis();

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {

            String sql = "CREATE TABLE IF NOT EXISTS auth " +
                    "(id SERIAL not NULL, " +
                    " name VARCHAR(49), " +
                    " pubID VARCHAR(129)) ";

            stmt.executeUpdate(sql);

            String copySql = "COPY auth(name, pubID) FROM STDIN WITH (FORMAT 'csv', DELIMITER E'\\t')";

            // create a copy manager instance
            CopyManager copyManager = new CopyManager((BaseConnection) conn);

            // open a stream to the file
            BufferedReader br = new BufferedReader(new FileReader(filePath));

            // Get the total number of lines in the file
            long totalLines = Files.lines(Paths.get(filePath)).count();

            // Initialize the progress bar
            ProgressBar progressBar = new ProgressBar("Progress", totalLines);
            progressBar.stepTo(totalLines);

            // copy data from the stream to the table using the copy manager
            copyManager.copyIn(copySql, br);

            System.out.println("Data imported successfully");

        } catch (Exception e) {
            e.printStackTrace();
        }

        long endTime = System.currentTimeMillis();
        long timeTaken = endTime - startTime;
        System.out.println("Time taken: " + timeTaken + " milliseconds");
        return timeTaken;
    }


    public static long loadWithBatchInsert(String url, String user, String password, String filePath) {
        long startTime = System.currentTimeMillis();

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement();
             BufferedReader br = new BufferedReader(new FileReader(filePath))) {

            String sql = "CREATE TABLE IF NOT EXISTS auth " +
                    "(id SERIAL not NULL, " +
                    " name VARCHAR(49), " +
                    " pubID VARCHAR(129)) ";

            stmt.executeUpdate(sql);

            String line;
            int batchSize = 1000;
            int count = 0;

            PreparedStatement pstmt = conn.prepareStatement("INSERT INTO auth (name, pubID) VALUES (?, ?)");

            // Get the total number of lines in the file
            long totalLines = Files.lines(Paths.get(filePath)).count();

            // Initialize the progress bar
            ProgressBar progressBar = new ProgressBar("Progress", totalLines);

            while ((line = br.readLine()) != null) {
                String[] values = line.split("\t");
                String name = values[0];
                String pubID = values[1];

                pstmt.setString(1, name);
                pstmt.setString(2, pubID);
                pstmt.addBatch();

                if (++count % batchSize == 0) {
                    pstmt.executeBatch();
                    progressBar.stepBy(batchSize);
                    progressBar.setExtraMessage(String.format("%.2f%%", (double)count * 100 / totalLines));
                }
            }

            pstmt.executeBatch();
            pstmt.close();

            System.out.println("Data imported successfully");

        } catch (Exception e) {
            e.printStackTrace();
        }

        long endTime = System.currentTimeMillis();
        long timeTaken = endTime - startTime;
        System.out.println("Time taken: " + timeTaken + " milliseconds");
        return timeTaken;
    }


    private static int getTotalLines(String filePath) throws IOException {
        int totalLines = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            while (reader.readLine() != null) totalLines++;
        }
        return totalLines;
    }



}