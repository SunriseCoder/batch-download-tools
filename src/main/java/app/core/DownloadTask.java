package app.core;

import app.util.StringUtils;

import java.io.IOException;
import java.util.concurrent.Callable;

public class DownloadTask implements Callable<DownloadTask> {
    private final String fileLink;
    private final String fileName;
    private final String outputFolder;
    private boolean finishedSuccessfully;
    private String errorMessage;

    public DownloadTask(String fileLink, String outputFolder) {
        this.fileLink = fileLink;
        this.fileName = StringUtils.getFilenameFromURL(fileLink);
        this.outputFolder = outputFolder;
        finishedSuccessfully = false;
        errorMessage = "";
    }

    @Override
    public DownloadTask call() {
        try {
            HttpDownloader downloader = new HttpDownloader();
            downloader.downloadFileToFile(fileLink, outputFolder);
            finishedSuccessfully = true;
        } catch (IOException e) {
            errorMessage = e.getMessage();
        }

        return this;
    }

    public String getFileLink() {
        return fileLink;
    }

    public String getFileName() {
        return fileName;
    }

    public String getOutputFolder() {
        return outputFolder;
    }

    public boolean isFinishedSuccessfully() {
        return finishedSuccessfully;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
