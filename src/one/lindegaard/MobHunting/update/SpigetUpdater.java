package one.lindegaard.MobHunting.update;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;
import org.inventivetalent.spiget.downloader.SpigetDownloader;
import org.inventivetalent.update.spiget.SpigetUpdate;
import org.inventivetalent.update.spiget.UpdateCallback;
import org.inventivetalent.update.spiget.comparator.VersionComparator;
import org.spiget.client.SpigetDownload;

import one.lindegaard.Core.update.UpdateStatus;
import one.lindegaard.MobHunting.MobHunting;

public class SpigetUpdater {

	private MobHunting plugin;

	public SpigetUpdater(MobHunting plugin) {
		this.plugin = plugin;
	}

	private static SpigetUpdate spigetUpdate = null;
	private UpdateStatus updateAvailable = UpdateStatus.UNKNOWN;
	private String currentJarFile = "";
	private String newDownloadVersion = "";

	public SpigetUpdate getSpigetUpdate() {
		return spigetUpdate;
	}

	public UpdateStatus getUpdateAvailable() {
		return updateAvailable;
	}

	public void setUpdateAvailable(UpdateStatus b) {
		updateAvailable = b;
	}

	public String getCurrentJarFile() {
		return currentJarFile;
	}

	public void setCurrentJarFile(String name) {
		currentJarFile = name;
	}

	public String getNewDownloadVersion() {
		return newDownloadVersion;
	}

	public void setNewDownloadVersion(String newDownloadVersion) {
		this.newDownloadVersion = newDownloadVersion;
	}

	public void hourlyUpdateCheck(final CommandSender sender, boolean updateCheck, final boolean silent) {
		long seconds = MobHunting.getInstance().getConfigManager().checkEvery;
		if (seconds < 900) {
			Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "[MobHunting]" + ChatColor.RED
					+ "[Warning] check_every in your config.yml is too low. A low number can cause server crashes. The number is raised to 900 seconds = 15 minutes.");
			seconds = 900;
		}
		if (updateCheck) {
			new BukkitRunnable() {
				@Override
				public void run() {
					checkForUpdate(sender, false, false);
				}
			}.runTaskTimer(MobHunting.getInstance(), 30000L, seconds * 20L);
		}
	}

	/**
	 * Download a new version, add version number to the downloaded filename
	 * (filename-n.n.n) , and the rename the old version to ???.jar.oldnnn
	 * 
	 * @param sender
	 * @return
	 */
	private boolean downloadAndUpdateJar(CommandSender sender) {
		final String OS = System.getProperty("os.name");

		boolean succes = spigetUpdate.downloadUpdate();

		new BukkitRunnable() {
			int count = 0;

			@Override
			public void run() {
				if (count++ > 20) {
					Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "[MobHunting]" + ChatColor.RED
							+ " No updates found. (No response from server after 20s)");
					plugin.getMessages().senderSendMessage(sender, ChatColor.GREEN
							+ plugin.getMessages().getString("mobhunting.commands.update.could-not-update"));
					plugin.getMessages().debug("Update error: %s", spigetUpdate.getFailReason().toString());
					this.cancel();
				} else {
					// Wait for the response
					if (succes) {
						if (OS.indexOf("Win") >= 0) {
							File downloadedJar = new File("plugins/update/" + currentJarFile);
							File newJar = new File("plugins/update/MobHunting-" + newDownloadVersion + ".jar");
							if (newJar.exists())
								newJar.delete();
							downloadedJar.renameTo(newJar);
							plugin.getMessages().senderSendMessage(sender, ChatColor.GREEN
									+ plugin.getMessages().getString("mobhunting.commands.update.complete"));
						} else {
							if (updateAvailable != UpdateStatus.RESTART_NEEDED) {
								File currentJar = new File("plugins/" + currentJarFile);
								File disabledJar = new File("plugins/" + currentJarFile + ".old");
								int count = 0;
								while (disabledJar.exists() && count++ < 100) {
									disabledJar = new File("plugins/" + currentJarFile + ".old" + count);
								}
								if (!disabledJar.exists()) {
									currentJar.renameTo(disabledJar);
									File downloadedJar = new File("plugins/update/" + currentJarFile);
									File newJar = new File("plugins/MobHunting-" + newDownloadVersion + ".jar");
									downloadedJar.renameTo(newJar);
									plugin.getMessages().debug("Moved plugins/update/" + currentJarFile
											+ " to plugins/MobHunting-" + newDownloadVersion + ".jar");
									updateAvailable = UpdateStatus.RESTART_NEEDED;
									plugin.getMessages().senderSendMessage(sender, ChatColor.GREEN
											+ plugin.getMessages().getString("mobhunting.commands.update.complete"));
								}
							}
						}
						this.cancel();
					}
				}
			}
		}.runTaskTimer(plugin, 20L, 20L);
		return true;
	}

	/**
	 * Check is a new version is available
	 * 
	 * @param sender
	 * @param updateCheck
	 * @param silent      - if true the player will not get the status in Game
	 */
	public void checkForUpdate(final CommandSender sender, final boolean silent, boolean forceDownload) {
		if (!silent)
			Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "[MobHunting] " + ChatColor.RESET
					+ plugin.getMessages().getString("mobhunting.commands.update.check"));
		if (updateAvailable != UpdateStatus.RESTART_NEEDED) {
			spigetUpdate = new SpigetUpdate(plugin, 3582);
			spigetUpdate.setVersionComparator(VersionComparator.EQUAL);
			// spigetUpdate.setUserAgent("MobHunting-" +
			// plugin.getDescription().getVersion());
			spigetUpdate.setUserAgent(
					"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.113 Safari/537.36");

			spigetUpdate.checkForUpdate(new UpdateCallback() {

				@Override
				public void updateAvailable(String newVersion, String downloadUrl, boolean hasDirectDownload) {
					//// VersionComparator.EQUAL handles all updates as new, so I have to check the
					//// version number manually
					updateAvailable = isUpdateNewerVersion(newVersion);
					boolean succes = false;
					if (updateAvailable == UpdateStatus.AVAILABLE) {
						newDownloadVersion = newVersion;
						sender.sendMessage(ChatColor.GOLD + "[MobHunting] " + ChatColor.GREEN + plugin.getMessages()
								.getString("mobhunting.commands.update.version-found", "newversion", newVersion));
						if (plugin.getConfigManager().autoupdate || forceDownload) {

							// downloadAndUpdateJar(sender);
							try {
								sender.sendMessage(ChatColor.GOLD + "[MobHunting] " + ChatColor.YELLOW + "agent="
										+ spigetUpdate.getUserAgent());
								spigetUpdate.downloadUpdate();
							} catch (Exception e) {
								// TODO: handle exception
								sender.sendMessage(ChatColor.GOLD + "[MobHunting] " + ChatColor.YELLOW
										+ "spigetDownload failed:" + e.getMessage());
							}

							if (hasDirectDownload) {

								// boolean succes = spigetUpdate.downloadUpdate();

								sender.sendMessage(ChatColor.GOLD + "[MobHunting] " + ChatColor.YELLOW + "DownloadUrl="
										+ downloadUrl);

								SpigetDownload download;
								try {
									download = new SpigetDownloader().download(downloadUrl);
									ReadableByteChannel channel = Channels.newChannel(download.getInputStream());
									FileOutputStream out = new FileOutputStream(
											"plugins/update/MobHunting-" + newDownloadVersion + ".jar");
									out.getChannel().transferFrom(channel, 0, Long.MAX_VALUE);
								} catch (FileNotFoundException e1) {
									e1.printStackTrace();
								} catch (IOException e1) {
									e1.printStackTrace();
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}

								if (succes) {

									final String OS = System.getProperty("os.name");

									new BukkitRunnable() {
										int count = 0;

										@Override
										public void run() {
											if (count++ > 20) {
												Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "[MobHunting]"
														+ ChatColor.RED
														+ " No updates found. (No response from server after 20s)");
												plugin.getMessages().senderSendMessage(sender,
														ChatColor.GREEN + plugin.getMessages().getString(
																"mobhunting.commands.update.could-not-update"));
												plugin.getMessages().debug("Update error: %s",
														spigetUpdate.getFailReason().toString());
												this.cancel();
											} else {
												// Wait for the response
												// if (succes) {
												if (OS.indexOf("Win") >= 0) {
													File downloadedJar = new File("plugins/update/" + currentJarFile);
													File newJar = new File(
															"plugins/update/MobHunting-" + newDownloadVersion + ".jar");
													if (newJar.exists())
														newJar.delete();
													downloadedJar.renameTo(newJar);
													plugin.getMessages().senderSendMessage(sender,
															ChatColor.GREEN + plugin.getMessages()
																	.getString("mobhunting.commands.update.complete"));
												} else {
													if (updateAvailable != UpdateStatus.RESTART_NEEDED) {
														File currentJar = new File("plugins/" + currentJarFile);
														File disabledJar = new File(
																"plugins/" + currentJarFile + ".old");
														int count = 0;
														while (disabledJar.exists() && count++ < 100) {
															disabledJar = new File(
																	"plugins/" + currentJarFile + ".old" + count);
														}
														if (!disabledJar.exists()) {
															currentJar.renameTo(disabledJar);
															File downloadedJar = new File(
																	"plugins/update/" + currentJarFile);
															File newJar = new File("plugins/MobHunting-"
																	+ newDownloadVersion + ".jar");
															downloadedJar.renameTo(newJar);
															plugin.getMessages()
																	.debug("Moved plugins/update/" + currentJarFile
																			+ " to plugins/MobHunting-"
																			+ newDownloadVersion + ".jar");
															updateAvailable = UpdateStatus.RESTART_NEEDED;
															plugin.getMessages().senderSendMessage(sender,
																	ChatColor.GREEN + plugin.getMessages().getString(
																			"mobhunting.commands.update.complete"));
														}
													}
												}
												this.cancel();
											}
											// }
										}
									}.runTaskTimer(plugin, 20L, 20L);

									// Update downloaded, will be loaded when the server restarts
								} else {
									// Update failed
									Bukkit.getLogger().warning(
											"Update download failed, reason is " + spigetUpdate.getFailReason());
								}
								sender.sendMessage(ChatColor.GOLD + "[MobHunting] " + ChatColor.GREEN
										+ plugin.getMessages().getString("mobhunting.commands.update.complete"));
							}

						} else
							sender.sendMessage(ChatColor.GOLD + "[MobHunting] " + ChatColor.GREEN
									+ plugin.getMessages().getString("mobhunting.commands.update.help"));
					}
				}

				@Override
				public void upToDate() {
					//// Plugin is up-to-date
					if (!silent)
						sender.sendMessage(ChatColor.GOLD + "[MobHunting] " + ChatColor.RESET
								+ plugin.getMessages().getString("mobhunting.commands.update.no-update"));
				}
			});
		}
	}

	/**
	 * Check if "newVersion" is newer than plugin's current version
	 * 
	 * @param newVersion
	 * @return
	 */
	public UpdateStatus isUpdateNewerVersion(String newVersion) {
		// Version format on Spigot.org & Spiget.org: "n.n.n"
		// Version format in jar file: "n.n.n" | "n.n.n-SNAPSHOT-Bn"

		int updateCheck = 0, pluginCheck = 0;
		boolean snapshot = false;
		String[] updateVer = newVersion.split("\\.");

		// Check the version #'s
		String[] pluginVerSNAPSHOT = plugin.getDescription().getVersion().split("\\-");
		if (pluginVerSNAPSHOT.length > 1)
			snapshot = pluginVerSNAPSHOT[1].equals("SNAPSHOT");
		if (snapshot)
			plugin.getMessages().debug("You are using a development version (%s)",
					plugin.getDescription().getVersion());
		String[] pluginVer = pluginVerSNAPSHOT[0].split("\\.");
		// Run through major, minor, sub - version numbers
		for (int i = 0; i < Math.max(updateVer.length, pluginVer.length); i++) {
			try {
				updateCheck = 0;
				if (i < updateVer.length)
					updateCheck = Integer.valueOf(updateVer[i]);
				pluginCheck = 0;
				if (i < pluginVer.length)
					pluginCheck = Integer.valueOf(pluginVer[i]);
				if (updateCheck > pluginCheck) {
					return UpdateStatus.AVAILABLE;
				} else if (updateCheck < pluginCheck)
					return UpdateStatus.NOT_AVAILABLE;
			} catch (Exception e) {
				plugin.getLogger().warning("Could not determine update's version # ");
				plugin.getLogger().warning("Installed plugin version: " + plugin.getDescription().getVersion());
				plugin.getLogger().warning("Newest version on Spiget.org: " + newVersion);
				return UpdateStatus.UNKNOWN;
			}
		}
		if ((updateCheck == pluginCheck && snapshot))
			return UpdateStatus.AVAILABLE;
		else
			return UpdateStatus.NOT_AVAILABLE;
	}

}
