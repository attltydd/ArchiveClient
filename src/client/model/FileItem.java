package client.model;

public class FileItem {

    private final String name;
    private final long size;

    public FileItem(String name, long size) {
        this.name = name;
        this.size = size;
    }

    public String getName() {
        return name;
    }

    public long getSize() {
        return size;
    }
}