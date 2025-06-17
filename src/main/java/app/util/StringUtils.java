package app.util;

public class StringUtils {
    public static String getFilenameFromURL(String fileLink) {
        int slashIndex = fileLink.lastIndexOf("/");
        if (slashIndex == -1) {
            return fileLink;
        }

        return fileLink.substring(slashIndex + 1);
    }
}
