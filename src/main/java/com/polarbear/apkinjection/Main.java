package com.polarbear.apkinjection;

import org.apache.commons.cli.*;

import java.io.File;

public class Main {
    public static void main(String[] args) {
        System.out.println("Welcome! (polarbear apk Injection)");
        Options options = buildOptions();
        CommandLineParser parser = new DefaultParser();
        CommandLine cli;
        try {
            cli = parser.parse(options, args);
            if (cli.hasOption("h")) {
                HelpFormatter hf = new HelpFormatter();
                hf.printHelp("Options", options);
                return;
            }
            if (!checkCommandLine(cli)) {
                return;
            }
            new HandleTask(cli.getOptionValue("s"), cli.getOptionValue("t"), cli.getOptionValue("c"), cli.getOptionValue("n"), cli.getOptionValue("p"), cli.getOptionValue("k")).run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean checkCommandLine(CommandLine cli) {
        if (!cli.hasOption("s")) {
            System.out.println("require source apk file！！！");
            return false;
        }
        if (!cli.hasOption("t")) {
            System.out.println("require target apk file！！！");
            return false;
        }
        if (!cli.hasOption("c")) {
            System.out.println("require target class ！！！");
            return false;
        }
        if (!new File(cli.getOptionValue("s")).isFile()) {
            System.out.println("not found source apk file！！！");
            return false;
        }
        if (!new File(cli.getOptionValue("t")).isFile()) {
            System.out.println("not found target apk file！！！");
            return false;
        }
        if (!cli.hasOption("k")) {
            System.out.println("require ketstore file！！！");
            return false;
        }
        if (!cli.hasOption("n")) {
            System.out.println("require key name！！！");
            return false;
        }
        if (!cli.hasOption("p")) {
            System.out.println("require key password ！！！");
            return false;
        }
        return true;
    }

    public static Options buildOptions() {
        Options options = new Options();
        options.addOption("s", "source", true, "source apk file");
        options.addOption("t", "target", true, "target apk file");
        options.addOption("c", "class", true, "target apk class");
        options.addOption("k", "keystore", true, "keystore file");
        options.addOption("n", "keyname", true, "keyname");
        options.addOption("p", "keypassword", true, "keypassword");
        options.addOption("h", "help");
        return options;
    }
}