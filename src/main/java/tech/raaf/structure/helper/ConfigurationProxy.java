package tech.raaf.structure.helper;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang.Validate;

import java.util.Optional;
import java.util.function.Function;

/**
 * ConfigurationProxy sits in front of Apache Commons CLI and a helper method that gets values from the environment
 * or the java properties if the value is not available in the environment.
 * This is done so we have a single API for retrieving configuration values. A better solution would be to implement
 * something like Commons Configuration, but this is a quicker fix and we're time constrained.
 */
public class ConfigurationProxy {

    /**
     * The parsed command line options. These options are parsed according to {@link ConfigurationProxyBuilder#options}.
     */
    private final CommandLine commandLineOptions;
    /**
     * A function that takes a string and outputs a string. Used when looking up keys in the environment.
     */
    private final Function<String, String> keyTransform;

    /**
     * A builder for {@link ConfigurationProxy}.
     */
    public static class ConfigurationProxyBuilder {
        /**
         * The commons cli options we'll proxy for.
         */
        private Options options;
        /**
         * The arguments passed to this program on the command line.
         */
        private String[] args;
        /**
         * We might want to transform keys before looking them up in the environment.
         * Example: transform "keystore-password" to "KEYSTORE_PASSWORD".
         */
        private Function<String, String> keyTransform;

        /**
         * See {@link ConfigurationProxyBuilder#options}.
         * @param options See {@link ConfigurationProxyBuilder#options}.
         * @return This builder.
         */
        public ConfigurationProxyBuilder options(Options options) {
            this.options = options;
            return this;
        }

        /**
         * The arguments that will be parsed with {@link ConfigurationProxyBuilder#options}.
         * @param args A list of arguments.
         * @return This builder.
         */
        public ConfigurationProxyBuilder args(String[] args) {
            this.args = args;
            return this;
        }

        /**
         * See {@link ConfigurationProxy#keyTransform}.
         * @param keyTransform See {@link ConfigurationProxy#keyTransform}.
         * @return This builder.
         */
        public ConfigurationProxyBuilder transformer(Function<String, String> keyTransform) {
            this.keyTransform = keyTransform;
            return this;
        }

        /**
         * Build a {@link ConfigurationProxy} according to the given settings.
         * @return A {@link ConfigurationProxy}.
         * @throws ParseException Thrown when we can't parse {@link ConfigurationProxyBuilder#args} with
         * {@link ConfigurationProxyBuilder#options}.
         */
        public ConfigurationProxy build() throws ParseException {
            Validate.notNull(options, "Cannot build a ConfigurationProxy without options.");
            Validate.notNull(args, "Cannot build a ConfigurationProxy without arguments.");

            // If we don't have a function to transform keys, use identity.
            if (keyTransform == null) {
                keyTransform = Function.identity();
            }

            return new ConfigurationProxy(new DefaultParser().parse(options, args), keyTransform);
        }
    }

    /* Used by ConfigurationProxyBuilder */
    private ConfigurationProxy(CommandLine commandLineOptions, Function<String, String> keyTransform) {
        this.commandLineOptions = commandLineOptions;
        this.keyTransform = keyTransform;
    }

    /**
     * Return a ConfigurationProxyBuilder, used to construct a ConfigurationProxy.
     * @return A new ConfigurationProxyBuilder.
     */
    public static ConfigurationProxyBuilder builder() {
        return new ConfigurationProxyBuilder();
    }

    /**
     * Get a configuration property, giving priority to command line options, environment variables, then properties.
     * @param configurationKey The name of the configuration key
     * @return The value of the configuration key, or null if there was no configuration
     */
    public Optional<String> getConfiguration(String configurationKey) {
        if (commandLineOptions.hasOption(configurationKey)) {
            /*
            Get the value, and if it's null (in case of a toggle option), put it in the Optional as an empty
            string. This is because Java 8's Optional doesn't allow us to put a null value in an Optional.
            See https://developer.atlassian.com/blog/2015/08/optional-broken/ for more information.
             */
            final String value = commandLineOptions.getOptionValue(configurationKey);
            return Optional.of(value == null ? "" : value);
        } else if (System.getenv(keyTransform.apply(configurationKey)) != null) {
            return Optional.of(System.getenv(keyTransform.apply(configurationKey)));
        } else if (System.getProperty(configurationKey) != null){
            return Optional.of(System.getProperty(configurationKey));
        } else {
            return Optional.empty();
        }
    }

}
