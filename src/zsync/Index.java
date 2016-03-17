package zsync;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Index implements Serializable {

    private static final long serialVersionUID = 1L;

    public Index() {
    }

    private Map<String, IndexEntry> entryMap = new HashMap<String, IndexEntry>();

    public void setEntryMap(Map<String, IndexEntry> entryMap) {
        this.entryMap = entryMap;
    }

    public Map<String, IndexEntry> getEntryMap() {
        return entryMap;
    }

    public void saveToFile(File file) throws FileNotFoundException {
        BufferedOutputStream indexStream = new BufferedOutputStream(new FileOutputStream(file));
        XMLEncoder indexEncoder = new XMLEncoder(indexStream);
        indexEncoder.writeObject(this);
        indexEncoder.close();
    }

    public static Index loadFromFile(File file) throws FileNotFoundException {
        BufferedInputStream indexStream = new BufferedInputStream(new FileInputStream(file));
        XMLDecoder indexDecoder = new XMLDecoder(indexStream);
        Index index = (Index) indexDecoder.readObject();
        indexDecoder.close();
        return index;
    }
}
