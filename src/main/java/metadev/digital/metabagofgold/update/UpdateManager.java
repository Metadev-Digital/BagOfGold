package metadev.digital.metabagofgold.update;

import metadev.digital.metabagofgold.BagOfGold;

import java.util.concurrent.ExecutionException;

public class UpdateManager {
    private BagOfGold plugin;
    private UpdateChecker pluginUpdateChecker;
    private UpdateChecker.UpdateResult lastResult;

    public UpdateManager(BagOfGold plugin) {
        this.plugin = plugin;
        this.lastResult = null;

        this.pluginUpdateChecker = UpdateChecker.init(this.plugin, 117870);
        isInitialized();
    }

    public void isInitialized() {
        if(UpdateChecker.isInitialized()){
            plugin.getMessages().debug("Update checker has been properly initialized");
        }
        else{
            plugin.getMessages().error(plugin.getMessages().getString("bagofgold.commands.update.fail"));
        }
    }

    private UpdateChecker.UpdateResult handleUpdateCheck() {
        if (UpdateChecker.isInitialized()){
            plugin.getMessages().notice("Checking SpigotMc.org for available updates...");
            UpdateChecker.UpdateResult lastRanResult = pluginUpdateChecker.getLastResult();

            // If it hasn't been run, or it gave an unsatisfactory answer last time it was called, ping the API again
            // Catch and rerun in all cases where the status may have changed if a user has not restarted in some time
            if(lastRanResult == null || lastRanResult.getReason() == UpdateChecker.UpdateReason.UNKNOWN_ERROR
                    || lastRanResult.getReason() == UpdateChecker.UpdateReason.COULD_NOT_CONNECT
                    || lastRanResult.getReason() == UpdateChecker.UpdateReason.UNAUTHORIZED_QUERY
                    || lastRanResult.getReason() == UpdateChecker.UpdateReason.INVALID_JSON
                    || lastRanResult.getReason() == UpdateChecker.UpdateReason.UNRELEASED_VERSION
                    || lastRanResult.getReason() == UpdateChecker.UpdateReason.UP_TO_DATE
            ) {
                try {
                    return pluginUpdateChecker.requestUpdateCheck().get();
                }
                catch (ExecutionException | InterruptedException e) {
                    plugin.getMessages().debug("UpdateManager threw Exception or was Interrupted when pinging Spigot API");
                }
            }
        }
        return null;
    }

    public void processCheckResultInConsole() {
        this.lastResult = handleUpdateCheck();

        if(lastResult != null){
            switch (lastResult.getReason()){
                case NEW_UPDATE:
                    plugin.getMessages().warning("===============================================");
                    plugin.getMessages().warning("====      " + plugin.getMessages().getString("bagofgold.commands.update.header_update_available") + "    ====");
                    plugin.getMessages().warning("===============================================");
                    plugin.getMessages().warning(plugin.getMessages().getString("bagofgold.commands.update.new_update", "newversion", lastResult.getNewestVersion()));
                    break;
                case COULD_NOT_CONNECT:
                    plugin.getMessages().warning("===============================================");
                    plugin.getMessages().warning("====      " + plugin.getMessages().getString("bagofgold.commands.update.header") + "    ====");
                    plugin.getMessages().warning("===============================================");
                    plugin.getMessages().warning(plugin.getMessages().getString("bagofgold.commands.update.could-not-connect"));
                case INVALID_JSON:
                    plugin.getMessages().error("===============================================");
                    plugin.getMessages().error("====      " + plugin.getMessages().getString("bagofgold.commands.update.header") + "    ====");
                    plugin.getMessages().error("===============================================");
                    plugin.getMessages().error(plugin.getMessages().getString("bagofgold.commands.update.invalid"));
                case UNAUTHORIZED_QUERY:
                    plugin.getMessages().error("===============================================");
                    plugin.getMessages().error("====      " + plugin.getMessages().getString("bagofgold.commands.update.header") + "    ====");
                    plugin.getMessages().error("===============================================");
                    plugin.getMessages().error(plugin.getMessages().getString("bagofgold.commands.update.unauthorized"));
                    break;
                case UNRELEASED_VERSION:
                    plugin.getMessages().notice("===============================================");
                    plugin.getMessages().notice("====      " + plugin.getMessages().getString("bagofgold.commands.update.header_development") + "    ====");
                    plugin.getMessages().notice("===============================================");
                    plugin.getMessages().notice(plugin.getMessages().getString("bagofgold.commands.update.development", "currentversion", lastResult.getNewestVersion()));
                    break;
                case UNKNOWN_ERROR, UNSUPPORTED_VERSION_SCHEME:
                    plugin.getMessages().error("===============================================");
                    plugin.getMessages().error("====      " + plugin.getMessages().getString("bagofgold.commands.update.header") + "    ====");
                    plugin.getMessages().error("===============================================");
                    plugin.getMessages().error(plugin.getMessages().getString("bagofgold.commands.update.unknown"));
                    break;
                case UP_TO_DATE:
                    plugin.getMessages().notice(plugin.getMessages().getString("bagofgold.commands.update.no_update"));
                    break;
            }
        }
    }

    public String processCheckResultInChat() {
        this.lastResult = handleUpdateCheck();
        if(lastResult != null){
            switch (lastResult.getReason()) {
                case UP_TO_DATE:
                    return plugin.getMessages().getString("bagofgold.commands.update.no_update");
                case NEW_UPDATE:
                    return plugin.getMessages().getString("bagofgold.commands.update.new_update", "newversion", lastResult.getNewestVersion());
                case COULD_NOT_CONNECT:
                    return plugin.getMessages().getString("bagofgold.commands.update.could-not-connect");
                case INVALID_JSON:
                    return plugin.getMessages().getString("bagofgold.commands.update.invalid");
                case UNAUTHORIZED_QUERY:
                    return plugin.getMessages().getString("bagofgold.commands.update.unauthorized");
                case UNRELEASED_VERSION:
                    return plugin.getMessages().getString("bagofgold.commands.update.development", "currentversion", lastResult.getNewestVersion());
                case UNKNOWN_ERROR, UNSUPPORTED_VERSION_SCHEME:
                    return plugin.getMessages().getString("bagofgold.commands.update.unknown");
            }
        }
        return plugin.getMessages().getString("bagofgold.commands.update.fail");
    }
}
