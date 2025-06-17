package app;

import app.core.HttpDownloader;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class DownloadFilesFromPageApp {
    public static void main(String[] args) throws IOException {
        System.out.println("Application started");

        if (args.length < 2) {
            printUsage();
            System.exit(1);
        }

        String pageLink = args[0];
        String filesExt = args[1];
        String outputFolder = args[2];

        Path outputFolderPath = Paths.get(outputFolder);
        if (Files.notExists(outputFolderPath)) {
            Files.createDirectories(outputFolderPath);
        }

        HttpDownloader downloader = new HttpDownloader();
        downloader.setDotProgress(1024 * 1024);

        System.out.println("Downloading page: " + pageLink);
        String pageContent = downloader.downloadPageAsString(pageLink);

        List<String> fileLinks = parsePage(pageContent, filesExt);
        for (String fileLink : fileLinks) {
            System.out.println("Downloading file: " + fileLink);
            downloader.downloadFileToFile(fileLink, outputFolder);
        }

        System.out.println("Done");
    }

    public static List<String> parsePage(String pageContent, String fileExt) {
        List<String> fileLinks = new ArrayList<>();

        Document document = Jsoup.parse(pageContent);
        Elements links = document.select("a[href]");
        for (Element link : links) {
            String href = link.attr("href");
            if (href.endsWith(fileExt)) {
                fileLinks.add(href);
            }
        }

        return fileLinks;
    }

    public static void printUsage() {
        System.out.println("Usage: " +
                DownloadFilesFromPageApp.class.getName() + " <page-link> <files-extension> <output-folder>");
    }
}
