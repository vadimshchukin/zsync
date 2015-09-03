package zsync;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Files {
    public static List<File> walk(String path) {
        List<File> fileList = new ArrayList<File>();
        walk(path, fileList);
        return fileList;
    }

    private static void walk(String path, List<File> fileList) {

        File root = new File(path);

        File[] fileArray = root.listFiles();
        if (fileArray == null) {
            return;
        }

        for (File file : fileArray) {
            fileList.add(file);

            if (file.isDirectory()) {
                walk(file.getPath(), fileList);
            }
        }
    }
}
