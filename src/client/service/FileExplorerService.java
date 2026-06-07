package client.service;

import client.model.FileItem;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileExplorerService {

    public List<FileItem> loadDirectory(File directory) {

        List<FileItem> result = new ArrayList<>();

        if (directory == null || !directory.isDirectory()) {
            return result;
        }

        File[] files = directory.listFiles();

        if (files == null) {
            return result;
        }

        for (File file : files) {

            result.add(
                new FileItem(
                    file.getName(),
                    file.length()
                )
            );

        }

        return result;
    }
}