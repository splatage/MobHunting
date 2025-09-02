package metadev.digital.MetaMobHunting.config;

import metadev.digital.MetaMobHunting.Messages.MessageHelper;
import metadev.digital.MetaMobHunting.MobHunting;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class Migrator {
    public static void moveLegacyConfiguration(File source, File target) {
        if (source.exists()) {
            MessageHelper.warning("Found legacy MobHunting configuration files. Attempting migration....");
            try {
                Files.move(source.toPath(), target.toPath(), REPLACE_EXISTING);
                MessageHelper.warning("Legacy MobHunting configuration successfully migrated.");
                MessageHelper.warning("============================================================");
                MessageHelper.warning("==== Config now located in the directory MetaMobHunting ====");
                MessageHelper.warning("============================================================");
            }
            catch (SecurityException e) {
                MessageHelper.error("Failed to migrate legacy MobHunting configuration files due to a security restriction on your system. Config must be manually updated.");
                throw new MigratorException();
            }
            catch (NullPointerException e) {
                MessageHelper.error("Failed to migrate legacy MobHunting configuration files due to NPE. Check your settings manually, as you may now be unexpectedly running a fresh install.");
                throw new MigratorException();
            }
            catch (IOException e) {
                MessageHelper.error("Failed to migrate legacy MobHunting configuration files. Check your settings manually, as you may now be unexpectedly running a fresh install.");
                throw new MigratorException();
            }
        }
        else {
            throw new MigratorException();
        }
    }
}


