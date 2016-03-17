package zsync;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class Application {

    private Options createCommandLineOptions() {

        Options options = new Options();

        options.addOption("h", "help", false, "print this help and exit");

        options.addOption("s", "hostname", true, "FTP hostname");
        options.addOption("u", "username", true, "FTP username");

        Option option = new Option("p", "password", true, "FTP password");
        option.setRequired(true);
        options.addOption(option);

        option = new Option("l", "local-root", true, "local root");
        option.setRequired(true);
        options.addOption(option);

        option = new Option("r", "remote-root", true, "remote root");
        option.setRequired(true);
        options.addOption(option);

        options.addOption("i", "index-file", true, "index file");
        options.addOption("v", "verbose", false, "verbose");
        options.addOption("i", "list", false, "list");
        options.addOption("o", "upload", false, "upload");

        return options;
    }
    
    private String hostname;
    private String username;
    private String password;
    private String localRootPath;
    private String remoteRootPath;
    private String indexFileName;
    private boolean verbose;
    private boolean list;
    private boolean upload;
    
    private void processComandLineOptions(String commandLineArguments[]) throws ParseException {
        CommandLineParser parser = new DefaultParser();

        Options options = new Options();
        options.addOption("h", "help", false, "print this help and exit");

        CommandLine commandLine = parser.parse(options, commandLineArguments, true);

        if (commandLine.hasOption("help")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.setOptionComparator(null);
            formatter.printHelp("zsync", createCommandLineOptions());
            return;
        }

        commandLine = parser.parse(createCommandLineOptions(), commandLineArguments);

        hostname = null;
        if (commandLine.hasOption("hostname")) {
            hostname = commandLine.getOptionValue("hostname");
        } else {
            hostname = "localhost";
        }

        username = null;
        if (commandLine.hasOption("username")) {
            username = commandLine.getOptionValue("username");
        } else {
            username = System.getProperty("user.name");
        }

        password = commandLine.getOptionValue("password");
        localRootPath = commandLine.getOptionValue("local-root");
        remoteRootPath = commandLine.getOptionValue("remote-root");

        indexFileName = null;
        if (commandLine.hasOption("index-file")) {
            indexFileName = commandLine.getOptionValue("index-file");
        } else {
            indexFileName = "zsync.xml";
        }

        verbose = commandLine.hasOption("verbose");
        upload = commandLine.hasOption("upload");
        list = commandLine.hasOption("list");
    }
    
    public String filePathToDatasetName(String filePath) {
        String directories[] = filePath.split("\\" + File.separator);

        String datasetName = "";
        for (int directoryIndex = 0; directoryIndex < directories.length - 1; directoryIndex++) {
            String directory = directories[directoryIndex];
            if (directoryIndex > 0) {
                datasetName += ".";
            }
            datasetName += String.format("%.8s", directory);
        }

        String memberName;
        String fileName = directories[directories.length - 1];
        int dotIndex = fileName.lastIndexOf(".");
        if (dotIndex == -1) {
            memberName = fileName;
        } else {
            memberName = String.format("%.8s", fileName.substring(0, dotIndex));
        }
        
        if (datasetName.isEmpty()) {
            datasetName = remoteRootPath;
        } else {
            datasetName = String.format("%s.%s", remoteRootPath, datasetName);
        }
        
        datasetName = String.format("%s(%s)", datasetName, memberName);
        datasetName = datasetName.toUpperCase();
        
        return datasetName;
    }
    
    private static String getDatasetNameWithoutMember(String datasetName) {
        int bracketIndex = datasetName.lastIndexOf("(");
        if (bracketIndex != -1) {
            datasetName = datasetName.substring(0, bracketIndex);
        }
        return datasetName;
    }
    
    private FTPClient clientFTP = new FTPClient();
    private Index index;
    boolean filesChanged;
    
    private void synchronizeFiles() throws Exception {
        if (verbose) {
            System.out.format("processing '%s' directory files%n", localRootPath);
        }
        List<File> files = Files.walk(localRootPath);
        List<File> changedFiles = new ArrayList<File>();
        for (File file : files) {
            if (!file.isFile()) {
                continue;
            }

            String filePath = file.getPath().substring(localRootPath.length() + 1);
            IndexEntry indexEntry = index.getEntryMap().get(filePath);
            if (indexEntry == null || file.lastModified() > indexEntry.getLastModified()) {
                changedFiles.add(file);
            }
        }
        
        List<String> removedFilePaths = new ArrayList<String>();
        for (Map.Entry<String, IndexEntry> entry : index.getEntryMap().entrySet()) {
            
            boolean fileFound = (new File(localRootPath, entry.getKey())).exists();
            
            if (!fileFound) {
                removedFilePaths.add(entry.getKey());
            }
        }

        filesChanged = changedFiles.size() > 0 || removedFilePaths.size() > 0;
        if (!filesChanged) {
            System.out.println("no files has been added or changed or removed");
            return;
        }
        
        if (verbose) {
            System.out.format("connecting to '%s' host%n", hostname);
        }
        clientFTP.connect(hostname);
        if (verbose) {
            System.out.format("logging in '%s' host as '%s' user%n", hostname, username);
        }
        clientFTP.login(username, password);

        for (File file : changedFiles) {

            String filePath = file.getPath().substring(localRootPath.length() + 1);

            if (upload) {
                String datasetName = filePathToDatasetName(filePath);
                String directoryName = getDatasetNameWithoutMember(datasetName);
                System.out.format("uploading '%s' file to '%s' data set%n", filePath, directoryName);
                BufferedInputStream fileStream = new BufferedInputStream(new FileInputStream(file));
                if (!clientFTP.storeDataset(datasetName, fileStream)) {
                    String replyString = clientFTP.getReplyString();
                    if (replyString.indexOf("requests a nonexistent partitioned data set") != -1) {
                        System.out.println("upload has failed because data set does not exist");
                        
                        System.out.format("creating '%s' data set%n", directoryName);
                        clientFTP.createPDS(directoryName);
                        
                        System.out.format("uploading '%s' file to '%s' data set%n", filePath, directoryName);
                        clientFTP.storeDataset(datasetName, fileStream);
                    } else {
                        throw new Exception(replyString);
                    }
                }
            } else {
                System.out.format("'%s' file changed on %s%n", filePath,
                        new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date(file.lastModified())));
            }

            IndexEntry indexEntry = new IndexEntry();
            indexEntry.setPath(filePath);
            indexEntry.setLastModified(file.lastModified());
            index.getEntryMap().put(filePath, indexEntry);
        }
        
        for (String filePath : removedFilePaths) {
            String datasetName = filePathToDatasetName(filePath);
            String directoryName = getDatasetNameWithoutMember(datasetName);
            System.out.format("deleting '%s' file from '%s' data set%n", filePath, directoryName);
            if (!clientFTP.deleteDataset(datasetName)) {
                throw new Exception(clientFTP.getReplyString());
            }
            index.getEntryMap().remove(filePath);
        }

        if (clientFTP.isConnected()) {
            if (verbose) {
                System.out.format("logging out '%s' host%n", hostname);
            }
            clientFTP.logout();
        }
    }

    public void run(String arguments[]) throws Exception {
        
        processComandLineOptions(arguments);

        /* load file index */
        File indexFile = new File(indexFileName);
        if (indexFile.exists()) {
            if (verbose) {
                System.out.format("loading index from '%s' file%n", indexFileName);
            }
            index = Index.loadFromFile(indexFile);
        } else {
            index = new Index();
        }
        
        synchronizeFiles();

        /* save file index if neccesary */
        if (!list && filesChanged) {
            if (verbose) {
                System.out.format("saving index to '%s' file%n", indexFileName);
            }
            index.saveToFile(indexFile);
        }
    }

    public static void main(String arguments[]) {

        try {
            (new Application()).run(arguments);
        } catch (Exception error) {
            System.out.println(error.getMessage());
        }
    }
}
