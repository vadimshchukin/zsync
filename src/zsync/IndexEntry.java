package zsync;

import java.io.Serializable;

@SuppressWarnings("serial")
public class IndexEntry implements Serializable {

    public IndexEntry() {
    }

    private String path;

    public void setPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    private long lastModified;

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public long getLastModified() {
        return lastModified;
    }
}
