package zsync;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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

    public Options createOptions() {

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
    
    public static String filePathToDatasetName(String filePath) {
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
        
        datasetName = String.format("%s(%s)", datasetName, memberName);
        datasetName = datasetName.toUpperCase();
        
        return datasetName;
    }

    public void run(String arguments[]) throws IOException, ParseException {

        CommandLineParser parser = new DefaultParser();

        Options options = new Options();
        options.addOption("h", "help", false, "print this help and exit");

        CommandLine commandLine = parser.parse(options, arguments, true);

        if (commandLine.hasOption("help")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.setOptionComparator(null);
            formatter.printHelp("zsync", createOptions());
            return;
        }

        commandLine = parser.parse(createOptions(), arguments);

        String hostname = null;
        if (commandLine.hasOption("hostname")) {
            hostname = commandLine.getOptionValue("hostname");
        } else {
            hostname = "localhost";
        }

        String username = null;
        if (commandLine.hasOption("username")) {
            username = commandLine.getOptionValue("username");
        } else {
            username = System.getProperty("user.name");
        }

        String password = commandLine.getOptionValue("password");
        String localRootPath = commandLine.getOptionValue("local-root");
        String remoteRootPath = commandLine.getOptionValue("remote-root");

        String indexFileName = null;
        if (commandLine.hasOption("index-file")) {
            indexFileName = commandLine.getOptionValue("index-file");
        } else {
            indexFileName = "zsync.xml";
        }

        boolean verbose = commandLine.hasOption("verbose");

        FTPClient clientFTP = new FTPClient();

        Index index;
        File indexFile = new File(indexFileName);
        if (indexFile.exists()) {
            if (verbose) {
                System.out.format("loading index from '%s' file%n", indexFileName);
            }
            index = Index.loadFromFile(indexFile);
        } else {
            index = new Index();
        }

        if (verbose) {
            System.out.format("traversing '%s' directory files%n", localRootPath);
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

        if (changedFiles.size() > 0 || removedFilePaths.size() > 0) {
            if (verbose) {
                System.out.format("connecting to '%s'%n", hostname);
            }
            clientFTP.connect(hostname);

            if (verbose) {
                System.out.format("logging in '%s' as '%s'%n", hostname, username);
            }
            clientFTP.login(username, password);
        }

        for (File file : changedFiles) {

            String filePath = file.getPath().substring(localRootPath.length() + 1);
            
            String datasetName = String.format("%s.%s", remoteRootPath, filePathToDatasetName(filePath));

            if (commandLine.hasOption("upload")) {
                System.out.format("uploading '%s' file to '%s' data set%n", filePath, datasetName);
                BufferedInputStream fileStream = new BufferedInputStream(new FileInputStream(file));
                clientFTP.storeDataset(datasetName, fileStream);
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
            String datasetName = String.format("%s.%s", remoteRootPath, filePathToDatasetName(filePath));
            System.out.format("deleting '%s' file from '%s' data set%n", filePath, datasetName);
            clientFTP.deleteDataset(datasetName);
            index.getEntryMap().remove(filePath);
        }

        if (clientFTP.isConnected()) {
            if (verbose) {
                System.out.format("logging out '%s'%n", hostname);
            }
            clientFTP.logout();
        }

        if (!commandLine.hasOption("list")) {
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
