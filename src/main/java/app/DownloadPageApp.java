package app;

import app.core.HttpDownloader;

import java.io.IOException;

public class DownloadPageApp {
    public static void main(String[] args) throws IOException {
        System.out.println("Application started");

        if (args.length < 2) {
            printUsage();
            System.exit(1);
        }

        String pageLink = args[0];
        String outputFile = args[1];

        HttpDownloader downloader = new HttpDownloader();
        downloader.setDotProgress(4096);

        System.out.println("Downloading page " + pageLink + " to " + outputFile);
        downloader.downloadPageToFile(pageLink, outputFile);

        System.out.println("Application finished");
    }

    private static void printUsage() {
        System.out.println("Usage: " +
                DownloadPageApp.class.getName() + " <page-link> <save-file>");
    }
}
