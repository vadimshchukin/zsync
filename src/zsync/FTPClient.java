package zsync;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class FTPClient extends org.apache.commons.net.ftp.FTPClient {
    
    private Map<String, Object> options = new HashMap<String, Object>();
    
    public void setOption(String name, Object value)
    {
        options.put(name, value);
    }
    
    private static String formatSiteCommand(Map<String, Object> options)
    {
        StringBuilder siteCommand = new StringBuilder();
        for (Map.Entry<String, Object> option : options.entrySet())
        {
            siteCommand.append(option.getKey());
            siteCommand.append("=");
            siteCommand.append(option.getValue());
            siteCommand.append(" ");
        }
        return siteCommand.toString();
    }
    
    private void executeSiteCommand(Map<String, Object> options) throws IOException
    {
        String siteCommand = formatSiteCommand(options);
        site(siteCommand);
    }
    
    public void applyOptions() throws IOException
    {
        executeSiteCommand(options);
    }
    
    public void createLibrary(String datasetName) throws IOException
    {
        makeDirectory(String.format("'%s'", datasetName));
    }
    
    public void deleteDataset(String datasetName) throws IOException
    {
        deleteFile(String.format("'%s'", datasetName));
    }
}
