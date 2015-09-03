package zsync;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.net.ftp.FTPClient;

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

        return options;
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

        FTPClient clientFTP = new FTPClient();

        Index index;
        File indexFile = new File(indexFileName);
        if (indexFile.exists()) {
            System.out.format("loading index from '%s' file%n", indexFileName);
            index = Index.loadFromFile(indexFile);
        } else {
            index = new Index();
        }

        System.out.format("traversing '%s' directory files%n", localRootPath);
        List<File> fileList = Files.walk(localRootPath);
        for (File file : fileList) {

            if (!file.isFile()) {
                continue;
            }

            String relativeFilePath = file.getPath().substring(localRootPath.length() + 1);
            IndexEntry indexEntry = index.getEntryMap().get(relativeFilePath);
            if (indexEntry != null && file.lastModified() > indexEntry.getLastModified()) {

                if (!clientFTP.isConnected()) {
                    System.out.format("connecting to '%s'%n", hostname);
                    clientFTP.connect(hostname);
                    
                    System.out.format("logging in '%s' as '%s'%n", hostname, username);
                    clientFTP.login(username, password);
                }

                String qualifier = file.getParentFile().getName().toUpperCase();

                String memberName = "";
                int dotIndex = file.getName().indexOf(".");
                if (dotIndex > 0) {
                    memberName = file.getName().substring(0, dotIndex).toUpperCase();
                }
                String datasetName = String.format("%s.%s(%s)", remoteRootPath, qualifier, memberName);

                System.out.format("uploading '%s' file to '%s' data set%n", relativeFilePath, datasetName);
                BufferedInputStream fileStream = new BufferedInputStream(new FileInputStream(file));
                clientFTP.storeFile(String.format("'%s'", datasetName), fileStream);
            }

            indexEntry = new IndexEntry();
            indexEntry.setPath(relativeFilePath);
            indexEntry.setLastModified(file.lastModified());
            index.getEntryMap().put(relativeFilePath, indexEntry);
        }
        
        if (clientFTP.isConnected()) {
            System.out.format("logging out '%s'%n", hostname);
            clientFTP.logout();
        }

        System.out.format("saving index to '%s' file%n", indexFileName);
        index.saveToFile(indexFile);
    }

    public static void main(String arguments[]) {

        try {
            (new Application()).run(arguments);
            ;
        } catch (Exception error) {
            System.out.println(error.getMessage());
        }
    }
}
