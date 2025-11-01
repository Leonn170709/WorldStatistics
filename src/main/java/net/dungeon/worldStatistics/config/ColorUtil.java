package net.dungeon.worldStatistics.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Unterstützt verschiedene Farbformate:
 * - Legacy (&a, &c, etc.)
 * - Hex (&#RRGGBB oder &#RGB)
 * - RGB (&{r,g,b})
 * - Gradienten ({#RRGGBB>>#RRGGBB}text{/#})
 * - MiniMessage (<gradient:#ff0000:#00ff00>text</gradient>)
 */
public class ColorUtil {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})");
    private static final Pattern RGB_PATTERN = Pattern.compile("&\\{(\\d{1,3}),(\\d{1,3}),(\\d{1,3})\\}");
    private static final Pattern GRADIENT_PATTERN = Pattern.compile("\\{#([A-Fa-f0-9]{6})>>#([A-Fa-f0-9]{6})\\}(.*?)\\{/#\\}");
    private static final Pattern MINI_MESSAGE_PATTERN = Pattern.compile("<[^>]+>");

    public static String translate(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }

        if (isMiniMessage(message)) {
            return translateMiniMessage(message);
        }

        message = applyGradient(message);
        message = applyRGB(message);
        message = applyHex(message);
        message = ChatColor.translateAlternateColorCodes('&', message);

        return message;
    }

    private static boolean isMiniMessage(String message) {
        return MINI_MESSAGE_PATTERN.matcher(message).find();
    }

    private static String translateMiniMessage(String message) {
        try {
            Component component = MINI_MESSAGE.deserialize(message);
            return LEGACY_SERIALIZER.serialize(component);
        } catch (Exception e) {
            return translate(message.replaceAll("<[^>]+>", ""));
        }
    }

    private static String applyHex(String message) {
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String hex = matcher.group(1);

            // Kurze Hex Codes expandieren (RGB -> RRGGBB)
            if (hex.length() == 3) {
                hex = String.valueOf(hex.charAt(0)) + hex.charAt(0) +
                        hex.charAt(1) + hex.charAt(1) +
                        hex.charAt(2) + hex.charAt(2);
            }

            matcher.appendReplacement(result, net.md_5.bungee.api.ChatColor.of("#" + hex).toString());
        }
        matcher.appendTail(result);

        return result.toString();
    }

    private static String applyRGB(String message) {
        Matcher matcher = RGB_PATTERN.matcher(message);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            try {
                int r = Math.min(255, Math.max(0, Integer.parseInt(matcher.group(1))));
                int g = Math.min(255, Math.max(0, Integer.parseInt(matcher.group(2))));
                int b = Math.min(255, Math.max(0, Integer.parseInt(matcher.group(3))));

                Color color = new Color(r, g, b);
                String hex = String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());

                matcher.appendReplacement(result, net.md_5.bungee.api.ChatColor.of(hex).toString());
            } catch (NumberFormatException e) {
                matcher.appendReplacement(result, matcher.group(0));
            }
        }
        matcher.appendTail(result);

        return result.toString();
    }

    private static String applyGradient(String message) {
        Matcher matcher = GRADIENT_PATTERN.matcher(message);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String startHex = matcher.group(1);
            String endHex = matcher.group(2);
            String text = matcher.group(3);

            String gradient = createGradient(text, startHex, endHex);
            matcher.appendReplacement(result, Matcher.quoteReplacement(gradient));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    private static String createGradient(String text, String startHex, String endHex) {
        Color start = Color.decode("#" + startHex);
        Color end = Color.decode("#" + endHex);

        StringBuilder gradient = new StringBuilder();
        int length = text.length();

        for (int i = 0; i < length; i++) {
            char c = text.charAt(i);

            if (c == ' ') {
                gradient.append(c);
                continue;
            }

            float ratio = (float) i / (float) (length - 1);
            int r = (int) (start.getRed() + ratio * (end.getRed() - start.getRed()));
            int g = (int) (start.getGreen() + ratio * (end.getGreen() - start.getGreen()));
            int b = (int) (start.getBlue() + ratio * (end.getBlue() - start.getBlue()));

            Color color = new Color(r, g, b);
            String hex = String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());

            gradient.append(net.md_5.bungee.api.ChatColor.of(hex)).append(c);
        }

        return gradient.toString();
    }

    public static String strip(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }

        message = message.replaceAll("<[^>]+>", "");
        message = message.replaceAll("\\{#[A-Fa-f0-9]{6}>>#[A-Fa-f0-9]{6}\\}", "")
                .replaceAll("\\{/#\\}", "");
        message = message.replaceAll("&#[A-Fa-f0-9]{6}", "")
                .replaceAll("&#[A-Fa-f0-9]{3}", "");
        message = message.replaceAll("&\\{\\d{1,3},\\d{1,3},\\d{1,3}\\}", "");
        message = ChatColor.stripColor(message);

        return message;
    }

    public static List<String> translate(List<String> messages) {
        List<String> result = new ArrayList<>();
        for (String message : messages) {
            result.add(translate(message));
        }
        return result;
    }
}