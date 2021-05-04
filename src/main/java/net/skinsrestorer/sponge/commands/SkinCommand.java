/*
 * #%L
 * SkinsRestorer
 * %%
 * Copyright (C) 2021 SkinsRestorer
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package net.skinsrestorer.sponge.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.MessageKeys;
import co.aikar.commands.annotation.*;
import co.aikar.commands.sponge.contexts.OnlinePlayer;
import net.skinsrestorer.api.PlayerWrapper;
import net.skinsrestorer.shared.exception.SkinRequestException;
import net.skinsrestorer.shared.storage.Config;
import net.skinsrestorer.shared.storage.CooldownStorage;
import net.skinsrestorer.shared.storage.Locale;
import net.skinsrestorer.shared.utils.C;
import net.skinsrestorer.shared.utils.log.SRLogger;
import net.skinsrestorer.sponge.SkinsRestorer;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;

import java.util.concurrent.TimeUnit;

@CommandAlias("skin")
@CommandPermission("%skin")
public class SkinCommand extends BaseCommand {
    private final SkinsRestorer plugin;
    private final SRLogger log;

    public SkinCommand(SkinsRestorer plugin) {
        this.plugin = plugin;
        log = plugin.getSrLogger();
    }

    @Default
    @SuppressWarnings({"deprecation"})
    public void onDefault(CommandSource source) {
        onHelp(source, getCurrentCommandManager().generateCommandHelp());
    }

    @Default
    @CommandPermission("%skinSet")
    @Description("%helpSkinSet")
    @Syntax("%SyntaxDefaultCommand")
    @SuppressWarnings({"unused"})
    public void onSkinSetShort(Player p, @Single String skin) {
        onSkinSetOther(p, new OnlinePlayer(p), skin, null);
    }

    @HelpCommand
    @Syntax(" [help]")
    public void onHelp(CommandSource source, CommandHelp help) {
        if (Config.USE_OLD_SKIN_HELP)
            sendHelp(source);
        else
            help.showHelp();
    }

    @Subcommand("clear")
    @CommandPermission("%skinClear")
    @Description("%helpSkinClear")
    @SuppressWarnings({"unused"})
    public void onSkinClear(Player p) {
        onSkinClearOther(p, new OnlinePlayer(p));
    }

    @Subcommand("clear")
    @CommandPermission("%skinClearOther")
    @CommandCompletion("@players")
    @Syntax("%SyntaxSkinClearOther")
    @Description("%helpSkinClearOther")
    public void onSkinClearOther(CommandSource source, @Single OnlinePlayer target) {
        Sponge.getScheduler().createAsyncExecutor(plugin).execute(() -> {
            if (!source.hasPermission("skinsrestorer.bypasscooldown") && CooldownStorage.hasCooldown(source.getName())) {
                source.sendMessage(plugin.parseMessage(Locale.SKIN_COOLDOWN.replace("%s", "" + CooldownStorage.getCooldown(source.getName()))));
                return;
            }

            final Player p = target.getPlayer();
            final String pName = p.getName();
            final String skin = plugin.getSkinStorage().getDefaultSkinNameIfEnabled(pName, true);

            // remove users defined skin from database
            plugin.getSkinStorage().removePlayerSkin(pName);

            if (setSkin(source, p, skin, false, true, null)) {
                if (source == p)
                    source.sendMessage(plugin.parseMessage(Locale.SKIN_CLEAR_SUCCESS));
                else
                    source.sendMessage(plugin.parseMessage(Locale.SKIN_CLEAR_ISSUER.replace("%player", pName)));
            }
        });
    }


    @Subcommand("update")
    @CommandPermission("%skinUpdate")
    @Description("%helpSkinUpdate")
    @SuppressWarnings({"unused"})
    public void onSkinUpdate(Player p) {
        onSkinUpdateOther(p, new OnlinePlayer(p));
    }

    @Subcommand("update")
    @CommandPermission("%skinUpdateOther")
    @CommandCompletion("@players")
    @Description("%helpSkinUpdateOther")
    @Syntax("%SyntaxSkinUpdateOther")
    public void onSkinUpdateOther(CommandSource source, @Single OnlinePlayer target) {
        Sponge.getScheduler().createAsyncExecutor(plugin).execute(() -> {
            if (!source.hasPermission("skinsrestorer.bypasscooldown") && CooldownStorage.hasCooldown(source.getName())) {
                source.sendMessage(plugin.parseMessage(Locale.SKIN_COOLDOWN.replace("%s", "" + CooldownStorage.getCooldown(source.getName()))));
                return;
            }

            final Player p = target.getPlayer();
            String skin = plugin.getSkinStorage().getPlayerSkin(p.getName());

            try {
                if (skin != null) {
                    //filter skinUrl
                    if (skin.startsWith(" ")) {
                        source.sendMessage(plugin.parseMessage(Locale.ERROR_UPDATING_URL));
                        return;
                    }

                    if (!plugin.getSkinStorage().updateSkinData(skin)) {
                        source.sendMessage(plugin.parseMessage(Locale.ERROR_UPDATING_SKIN));
                        return;
                    }

                } else {
                    // get DefaultSkin
                    skin = plugin.getSkinStorage().getDefaultSkinNameIfEnabled(p.getName(), true);
                }
            } catch (SkinRequestException e) {
                source.sendMessage(plugin.parseMessage(e.getMessage()));
                return;
            }

            if (setSkin(source, p, skin, false, false, null)) {
                if (source == p)
                    source.sendMessage(plugin.parseMessage(Locale.SUCCESS_UPDATING_SKIN_OTHER.replace("%player", p.getName())));
                else
                    source.sendMessage(plugin.parseMessage(Locale.SUCCESS_UPDATING_SKIN));
            }
        });
    }

    @Subcommand("set")
    @CommandPermission("%skinSet")
    @CommandCompletion("@skin")
    @Description("%helpSkinSet")
    @Syntax("%SyntaxSkinSet")
    public void onSkinSet(Player p, String[] skin) {
        if (skin.length > 0) {
            onSkinSetOther(p, new OnlinePlayer(p), skin[0], null);
        } else {
            throw new InvalidCommandArgument(true);
        }
    }

    @Subcommand("set")
    @CommandPermission("%skinSetOther")
    @CommandCompletion("@players @skin")
    @Description("%helpSkinSetOther")
    @Syntax("%SyntaxSkinSetOther")
    public void onSkinSetOther(CommandSource source, OnlinePlayer target, String skin, @Optional SkinType skinType) {
        Sponge.getScheduler().createAsyncExecutor(plugin).execute(() -> {
            final Player p = target.getPlayer();
            if (Config.PER_SKIN_PERMISSIONS && !source.hasPermission("skinsrestorer.skin." + skin)) {
                if (!source.hasPermission("skinsrestorer.ownskin") && !source.getName().equalsIgnoreCase(p.getName()) || !skin.equalsIgnoreCase(source.getName())) {
                    source.sendMessage(plugin.parseMessage(Locale.PLAYER_HAS_NO_PERMISSION_SKIN));
                    return;
                }
            }
            if (setSkin(source, p, skin, true, false, skinType) && (source != p))
                source.sendMessage(plugin.parseMessage(Locale.ADMIN_SET_SKIN.replace("%player", p.getName())));
        });
    }

    @Subcommand("url")
    @CommandPermission("%skinSetUrl")
    @CommandCompletion("@skinUrl")
    @Description("%helpSkinSetUrl")
    @Syntax("%SyntaxSkinUrl")
    @SuppressWarnings({"unused"})
    public void onSkinSetUrl(Player p, String url, @Optional SkinType skinType) {
        if (C.validUrl(url)) {
            onSkinSetOther(p, new OnlinePlayer(p), url, skinType);
        } else {
            p.sendMessage(plugin.parseMessage(Locale.ERROR_INVALID_URLSKIN));
        }
    }

    private boolean setSkin(CommandSource source, Player p, String skin) {
        return setSkin(source, p, skin, true, false, null);
    }

    // if save is false, we won't save the skin skin name
    // because default skin names shouldn't be saved as the users custom skin
    private boolean setSkin(CommandSource source, Player p, String skin, boolean save, boolean clear, SkinType skinType) {
        if (skin.equalsIgnoreCase("null")) {
            source.sendMessage(plugin.parseMessage(Locale.INVALID_PLAYER.replace("%player", skin)));
            return false;
        }

        if (Config.DISABLED_SKINS_ENABLED && !clear && !source.hasPermission("skinsrestorer.bypassdisabled")) {
            for (String dskin : Config.DISABLED_SKINS)
                if (skin.equalsIgnoreCase(dskin)) {
                    source.sendMessage(plugin.parseMessage(Locale.SKIN_DISABLED));
                    return false;
                }
        }

        final String senderName = source.getName();
        if (!source.hasPermission("skinsrestorer.bypasscooldown") && CooldownStorage.hasCooldown(senderName)) {
            source.sendMessage(plugin.parseMessage(Locale.SKIN_COOLDOWN.replace("%s", "" + CooldownStorage.getCooldown(senderName))));
            return false;
        }

        CooldownStorage.setCooldown(senderName, Config.SKIN_CHANGE_COOLDOWN, TimeUnit.SECONDS);

        final String pName = p.getName();
        final String oldSkinName = plugin.getSkinStorage().getPlayerSkin(pName);

        if (C.validUrl(skin)) {
            if (!source.hasPermission("skinsrestorer.command.set.url") && !Config.SKINWITHOUTPERM) {
                source.sendMessage(plugin.parseMessage(Locale.PLAYER_HAS_NO_PERMISSION_URL));
                CooldownStorage.resetCooldown(senderName);
                return false;
            }

            if (!C.allowedSkinUrl(skin)) {
                source.sendMessage(plugin.parseMessage(Locale.SKINURL_DISALLOWED));
                CooldownStorage.resetCooldown(senderName);
                return false;
            }

            try {
                source.sendMessage(plugin.parseMessage(Locale.MS_UPDATING_SKIN));
                String skinentry = " " + pName; // so won't overwrite premium playernames
                if (skinentry.length() > 16) // max len of 16 char
                    skinentry = skinentry.substring(0, 16);
                plugin.getSkinStorage().setSkinData(skinentry, plugin.getMineSkinAPI().genSkin(skin, String.valueOf(skinType)),
                        Long.toString(System.currentTimeMillis() + (100L * 365 * 24 * 60 * 60 * 1000))); // "generate" and save skin for 100 years
                plugin.getSkinStorage().setPlayerSkin(pName, skinentry); // set player to "whitespaced" name then reload skin
                plugin.getSkinApplierSponge().applySkin(new PlayerWrapper(p));
                if (!Locale.SKIN_CHANGE_SUCCESS.isEmpty() && !Locale.SKIN_CHANGE_SUCCESS.equals(Locale.PREFIX))
                    p.sendMessage(plugin.parseMessage(Locale.SKIN_CHANGE_SUCCESS));
                return true;
            } catch (SkinRequestException e) {
                source.sendMessage(plugin.parseMessage(e.getMessage()));
            }
        } else {
            try {
                if (save)
                    plugin.getSkinStorage().setPlayerSkin(pName, skin);
                plugin.getSkinApplierSponge().applySkin(new PlayerWrapper(p));
                if (!Locale.SKIN_CHANGE_SUCCESS.isEmpty() && !Locale.SKIN_CHANGE_SUCCESS.equals(Locale.PREFIX))
                    p.sendMessage(plugin.parseMessage(Locale.SKIN_CHANGE_SUCCESS));
                return true;
            } catch (SkinRequestException e) {
                source.sendMessage(plugin.parseMessage(e.getMessage()));
            }
        }
        CooldownStorage.setCooldown(senderName, Config.SKIN_ERROR_COOLDOWN, TimeUnit.SECONDS);
        rollback(p, oldSkinName, save);
        return false;
    }

    private void rollback(Player p, String oldSkinName, boolean save) {
        if (save)
            plugin.getSkinStorage().setPlayerSkin(p.getName(), oldSkinName != null ? oldSkinName : p.getName());
    }

    private void sendHelp(CommandSource source) {
        if (!Locale.SR_LINE.isEmpty())
            source.sendMessage(plugin.parseMessage(Locale.SR_LINE));
        source.sendMessage(plugin.parseMessage(Locale.HELP_PLAYER.replace("%ver%", plugin.getVersion())));
        if (!Locale.SR_LINE.isEmpty())
            source.sendMessage(plugin.parseMessage(Locale.SR_LINE));
    }

    @SuppressWarnings("unused")
    public enum SkinType {
        STEVE,
        SLIM,
    }
}
