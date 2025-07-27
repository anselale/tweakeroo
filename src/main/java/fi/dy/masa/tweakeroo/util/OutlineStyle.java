package fi.dy.masa.tweakeroo.util;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

public enum OutlineStyle implements IConfigOptionListEntry
{
    SOLID ("solid", "tweakeroo.label.outline_style.solid"),
    THICK ("thick", "tweakeroo.label.outline_style.thick");

    private final String configString;
    private final String translationKey;

    OutlineStyle(String configString, String translationKey)
    {
        this.configString = configString;
        this.translationKey = translationKey;
    }

    // IConfigOptionListEntry implementation
    @Override
    public String getStringValue()
    {
        return this.configString;
    }

    @Override
    public String getDisplayName()
    {
        return StringUtils.translate(this.translationKey);
    }

    @Override
    public IConfigOptionListEntry cycle(boolean forward)
    {
        int id = this.ordinal();

        if (forward)
        {
            if (++id >= values().length)
            {
                id = 0;
            }
        }
        else
        {
            if (--id < 0)
            {
                id = values().length - 1;
            }
        }

        return values()[id % values().length];
    }

    @Override
    public OutlineStyle fromString(String name)
    {
        return fromStringStatic(name);
    }

    public static OutlineStyle fromStringStatic(String name)
    {
        for (OutlineStyle style : OutlineStyle.values())
        {
            if (style.configString.equalsIgnoreCase(name))
            {
                return style;
            }
        }

        return OutlineStyle.SOLID;
    }
} 