package app.core;

import app.util.StringUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URI;

public class HttpDownloader {
    private long dotProgressPortion;

    public HttpDownloader() {
        dotProgressPortion = 0;
    }

    public void setDotProgress(long dotProgressPortion) {
        this.dotProgressPortion = dotProgressPortion;
    }

    public void downloadPageToFile(String pageLink, String outputFile) throws IOException {
        FileOutputStream outputStream = new FileOutputStream(outputFile);
        downloadPageToOutputStream(pageLink, outputStream);
    }

    public String downloadPageAsString(String pageLink) throws IOException {
        OutputStream outputStream = new ByteArrayOutputStream();
        downloadPageToOutputStream(pageLink, outputStream);
        return outputStream.toString();
    }

    private void downloadPageToOutputStream(String pageLink, OutputStream outputStream) throws IOException {
        long downloadedBytesFromLastDot = 0;

        URL url = URI.create(pageLink).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        int responseCode = connection.getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_OK) {
            InputStream inputStream = connection.getInputStream();

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);

                if (dotProgressPortion > 0) {
                    downloadedBytesFromLastDot += bytesRead;
                    if (downloadedBytesFromLastDot > dotProgressPortion) {
                        System.out.print(".");
                        downloadedBytesFromLastDot = 0;
                    }
                }
            }

            // Finalizing the line if we have printing progress ON
            if (dotProgressPortion > 0) {
                System.out.println();
            }

            outputStream.close();
            inputStream.close();
            connection.disconnect();
        } else {
            throw new IOException("HTTP error code : " + responseCode);
        }
    }

    public void downloadFileToFile(String fileLink, String outputFolder) throws IOException {
        long downloadedBytesFromLastDot = 0;
        long downloadedBytes = 0;

        URL downloadURL = URI.create(fileLink).toURL();
        String outputFilePath = outputFolder + File.separator + StringUtils.getFilenameFromURL(fileLink);
        String tmpFilePath = outputFilePath + ".tmp";

        // Retrieving Content-Length
        long contentLength;
        while (true) {
            try {
                HttpURLConnection connection = (HttpURLConnection) downloadURL.openConnection();
                int responseCode = connection.getResponseCode();

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw new IOException("HTTP error code : " + responseCode);
                }

                contentLength = connection.getContentLengthLong();
                if (contentLength == -1) {
                    throw new IOException("Content-Length: Unknown");
                }

                connection.disconnect();
                break;
            } catch (IOException e) {
                System.out.println(e.getClass() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        // Main Download Loop
        FileOutputStream outputStream = new FileOutputStream(tmpFilePath);
        while (downloadedBytes < contentLength) {
            try {
                // Opening a new Connection and requesting the Download Range
                HttpURLConnection connection = (HttpURLConnection) downloadURL.openConnection();
                connection.setRequestProperty("Range", "bytes=" + downloadedBytes + "-");
                int responseCode = connection.getResponseCode();

                if (responseCode != HttpURLConnection.HTTP_PARTIAL) {
                    throw new IOException("HTTP error code : " + responseCode);
                }

                InputStream inputStream = connection.getInputStream();

                int bytesRead = -1;
                byte[] buffer = new byte[4096];
                while ((downloadedBytes < contentLength) && (bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    downloadedBytes += bytesRead;

                    if (dotProgressPortion > 0) {
                        downloadedBytesFromLastDot += bytesRead;
                        if (downloadedBytesFromLastDot > dotProgressPortion) {
                            System.out.print(".");
                            downloadedBytesFromLastDot = 0;
                        }
                    }
                }

                // Finalizing the line if we have printing progress ON
                if (dotProgressPortion > 0) {
                    System.out.println();
                }

                outputStream.close();
                inputStream.close();
                connection.disconnect();
            } catch (IOException e) {
                System.out.println(e.getClass() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        // Renaming temporary file to result file after downloading is finished
        File file = new File(tmpFilePath);
        file.renameTo(new File(outputFilePath));
    }
}
