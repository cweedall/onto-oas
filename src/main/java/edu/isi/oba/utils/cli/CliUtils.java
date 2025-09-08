package edu.isi.oba.utils.cli;

import edu.isi.oba.utils.exithandler.FatalErrorHandler;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class CliUtils {

	private CliUtils() {
		throw new UnsupportedOperationException("Static utility class");
	}

	public static String getYamlConfigFileArgument(String[] args) {
		// obtain the options to pass configuration
		Options options = new Options();
		Option input = new Option("c", "config", true, "configuration file path");
		input.setRequired(true);
		options.addOption(input);

		CommandLineParser parser = new DefaultParser();
		HelpFormatter formatter = new HelpFormatter();
		CommandLine cmd;
		String config_yaml = null;

		try {
			cmd = parser.parse(options, args);
			config_yaml = cmd.getOptionValue("config");
		} catch (ParseException e) {
			FatalErrorHandler.fatal("utiConfiguration filelity-name", e);
		}

		return config_yaml;
	}
}
