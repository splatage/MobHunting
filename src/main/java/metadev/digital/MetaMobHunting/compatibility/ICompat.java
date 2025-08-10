package metadev.digital.MetaMobHunting.compatibility;

import metadev.digital.metacustomitemslib.compatibility.Feature;
import metadev.digital.metacustomitemslib.compatibility.exceptions.SpinupShutdownException;
import metadev.digital.MetaMobHunting.Messages.MessageHelper;
import metadev.digital.MetaMobHunting.Messages.constants.Prefixes;
import org.bukkit.plugin.Plugin;

public interface ICompat {
    void start() throws SpinupShutdownException;
    void shutdown() throws SpinupShutdownException;
    boolean isEnabled();
    boolean isSupported();
    boolean isActive();
    boolean isLoaded();
    String getPluginName();
    String getPluginVersion();
    Plugin getPluginInstance();

    /***
     * Standardized server debug messaging for enabling a plugin compatibility
     */
    default void detectedMessage(){
        MessageHelper.debug(this.getPluginName() + " detected. Validating support and attempting to enable plugin compatibility.");
    }

    /***
     * Standardized server messaging for a successfully enabled plugin compatibility
     */
    default void successfullyLoadedMessage(){
        MessageHelper.notice(this.getPluginName()+ " compatibility successfully loaded.");
    }

    /***
     * Standardized server debug messaging for a successfully shutdown plugin compatibility
     */
    default void successfullyShutdownMessage(){
        MessageHelper.debug(this.getPluginName() + " compatibility successfully shutdown.");
    }

    /***
     * Standardized server messaging for features which are not supported
     * @param unsupportedFeature - The feature in question that is unsupported
     */
    default void unsupportedMessage(Feature unsupportedFeature) {
        String intro = unsupportedFeature.getName().equals("base") ?
                "Your current version of " + this.getPluginName() + " ( " + this.getPluginVersion() + " ) "
                : "The requested feature ( " + unsupportedFeature.getName() + " ) ";

        MessageHelper.error(intro + " is not supported by your current versions of " + Prefixes.PLUGIN + " and Minecraft. " +
                "This feature is supported in version(s): " + unsupportedFeature.getConstraintsExplanationText());
    }

    /***
     * Standardized server messaging for features which have been disabled
     * @param disabledFeature - Feature this message is about which is disabled
     * @param helpText - String help text on what the enable criteria for this feature is when relevant
     */
    default void disabledMessage(Feature disabledFeature, String helpText) {
            String intro = disabledFeature.getName().equals("base") ?
                "Compatibility for " + this.getPluginName() + " "
                : "Compatibility for the feature " + disabledFeature.getName() + " ";

        MessageHelper.error(intro + " is currently disabled." + (!helpText.isEmpty() ? " To enable: " + helpText : ""));
    }

    /***
     * Standardized server messaging for plugin compatibilities which have run into a critical error
     * @param error - String value of any error help text
     */
    default void pluginError(String error){
        MessageHelper.error(" Compatibility with " + this.getPluginName() + " ran into a critical runtime error.");
        if(!error.isEmpty()) MessageHelper.error(error);
    }
}

