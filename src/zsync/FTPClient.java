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

    private static String formatSiteCommand(Map<String, Object> options) {

        StringBuilder siteCommand = new StringBuilder();

        for (Map.Entry<String, Object> option : options.entrySet()) {
            siteCommand.append(option.getKey());
            siteCommand.append("=");
            siteCommand.append(option.getValue());
            siteCommand.append(" ");
        }

        return siteCommand.toString();
    }

    private void executeSiteCommand(Map<String, Object> options) throws IOException {
        String siteCommand = formatSiteCommand(options);
        site(siteCommand);
    }

    public void applySiteOptions() throws IOException {
        executeSiteCommand(siteOptions);
    }

    private String formatDatasetName(String datasetName) {
        return String.format("'%s'", datasetName);
    }

    public boolean datasetExists(String datasetName) throws IOException {
        return listNames(formatDatasetName(datasetName)) != null;
    }

    public void createPDS(String datasetName) throws IOException {
        makeDirectory(formatDatasetName(datasetName));
    }

    public void deleteDataset(String datasetName) throws IOException {
        deleteFile(formatDatasetName(datasetName));
    }

    public boolean storeDataset(String datasetName, InputStream inputStream) throws IOException {
        return storeFile(formatDatasetName(datasetName), inputStream);
    }
}
