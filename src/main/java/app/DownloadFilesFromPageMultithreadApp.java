package app;

import app.core.DownloadTask;
import app.core.HttpDownloader;
import app.core.TaskWrap;
import app.util.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class DownloadFilesFromPageMultithreadApp {
    public static void main(String[] args) throws IOException {
        long startTime = System.currentTimeMillis();
        System.out.println("Application started");

        if (args.length < 3) {
            printUsage();
            System.exit(1);
        }

        String numberOfThreadsString = args[0];
        String pageLink = args[1];
        String filesExt = args[2];
        String outputFolder = args[3];

        int numberOfThreads = -1;
        try {
            numberOfThreads = Integer.parseInt(numberOfThreadsString);
            if (numberOfThreads < 0) {
                throw new InvalidParameterException("Number of threads must be a more than 0");
            }
            if (numberOfThreads == 0) {
                numberOfThreads = Runtime.getRuntime().availableProcessors();
            }
            System.out.println("Number of threads: " + numberOfThreads);
        } catch (Exception e) {
            System.err.println("Invalid number of threads: " + numberOfThreadsString);
            System.exit(1);
        }

        Path outputFolderPath = Paths.get(outputFolder);
        if (Files.notExists(outputFolderPath)) {
            Files.createDirectories(outputFolderPath);
        }

        HttpDownloader downloader = new HttpDownloader();
        downloader.setDotProgress(1024 * 1024);

        System.out.println("Downloading page: " + pageLink);
        String pageContent = downloader.downloadPageAsString(pageLink);

        List<String> fileLinks = parsePage(pageContent, filesExt);

        // Creating Tasks
        List<DownloadTask> tasksFinishedWithError = new ArrayList<>();
        try (ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);) {
            List<TaskWrap<DownloadTask>> taskWraps = new ArrayList<>();
            for (String fileLink : fileLinks) {
                System.out.println("Creating task for file: " + StringUtils.getFilenameFromURL(fileLink));
                DownloadTask task = new DownloadTask(fileLink, outputFolder);
                Future<DownloadTask> future = executorService.submit(task);
                taskWraps.add(new TaskWrap<>(task, future));
            }
            System.out.println("All the tasks have been created, waiting for them to finish");

            // Executing Tasks
            while (!taskWraps.isEmpty()) {
                Iterator<TaskWrap<DownloadTask>> iterator = taskWraps.iterator();
                while (iterator.hasNext()) {
                    TaskWrap<DownloadTask> taskWrap = iterator.next();
                    Future<DownloadTask> future = taskWrap.future();
                    if (future.isDone()) {
                        try {
                            DownloadTask task = future.get();
                            if (!task.isFinishedSuccessfully()) {
                                tasksFinishedWithError.add(task);
                            }
                        } catch (Exception e) {
                            tasksFinishedWithError.add(taskWrap.task());
                        }
                        iterator.remove();
                    }
                }

                // Waiting for the Tasks to be done
                System.out.println(taskWraps.size() + " task(s) left");
                if (!taskWraps.isEmpty()) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        // Just swallowing the exception
                    }
                }
            }
        }

        long workDuration = System.currentTimeMillis() - startTime;
        System.out.println("All the tasks have been done, took: " + workDuration  + " ms");
        if (!tasksFinishedWithError.isEmpty()) {
            System.out.println("Tasks finished with error for the following file(s):");
            System.out.println(
                    tasksFinishedWithError.stream()
                            .map(task -> task.getFileName() + ": " + task.getErrorMessage())
                            .collect(Collectors.joining("\n"))
            );
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
                DownloadFilesFromPageMultithreadApp.class.getName() +
                " <number-of-threads> <page-link> <files-extension> <output-folder>");
    }
}
