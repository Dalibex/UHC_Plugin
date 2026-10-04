package me.dalibex.UHC_DBasic.services;

import org.bukkit.Material;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.object.ObjectContents;

/** Builds the native Adventure component shown by an item roulette. */
public final class ItemRouletteRenderer {

    private ItemRouletteRenderer() {
    }

    public static Component render(Material material) {
        return render(material, NamedTextColor.WHITE);
    }

    public static Component render(Material material, TextColor nameColor,
                                   TextDecoration... nameDecorations) {
        ItemSpriteResolver.SpriteReference sprite = ItemSpriteResolver.resolve(material);
        Component icon = Component.object(ObjectContents.sprite(sprite.atlas(), sprite.sprite()));
        Component name = Component.translatable(ItemSpriteResolver.translationKey(material))
                .color(nameColor);
        for (TextDecoration decoration : nameDecorations) name = name.decorate(decoration);
        return icon.append(Component.space()).append(name)
                .decoration(TextDecoration.ITALIC, false);
    }

    public static Component replaceItem(Component template, Material material, TextColor nameColor,
                                        TextDecoration... nameDecorations) {
        Component item = render(material, nameColor, nameDecorations);
        return template.replaceText(TextReplacementConfig.builder()
                .matchLiteral("%item%")
                .replacement(item)
                .build());
    }
}
