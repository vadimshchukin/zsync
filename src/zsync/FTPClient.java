package zsync;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class FTPClient extends org.apache.commons.net.ftp.FTPClient {

    private Map<String, Object> siteOptions = new HashMap<String, Object>();

    public void setSiteOption(String name, Object value) {
        siteOptions.put(name, value);
    }

    private String formatSiteOptions() {
        StringBuilder siteCommand = new StringBuilder();
        for (Map.Entry<String, Object> option : siteOptions.entrySet()) {
            siteCommand.append(option.getKey());
            siteCommand.append("=");
            siteCommand.append(option.getValue());
            siteCommand.append(" ");
        }
        return siteCommand.toString();
    }

    public int executeSiteCommand() throws IOException {
        return site(formatSiteOptions());
    }

    private static String formatDatasetName(String datasetName) {
        return String.format("'%s'", datasetName);
    }

    public boolean datasetExists(String datasetName) throws IOException {
        return listNames(formatDatasetName(datasetName)) != null;
    }

    public boolean createPDS(String datasetName) throws IOException {
        return makeDirectory(formatDatasetName(datasetName));
    }

    public boolean deleteDataset(String datasetName) throws IOException {
        return deleteFile(formatDatasetName(datasetName));
    }

    public boolean storeDataset(String datasetName, InputStream inputStream) throws IOException {
        return storeFile(formatDatasetName(datasetName), inputStream);
    }
}
