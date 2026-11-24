package ro.abyssmc.abyssprotection;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import java.util.regex.*;

/** Converts &#RRGGBB and legacy & color/format sequences into chat components. */
public final class HexText {
    private static final Pattern TOKEN=Pattern.compile("(?i)&#([0-9a-f]{6})|&([0-9a-fk-or])");
    private HexText(){}
    public static Component parse(String raw){
        MutableComponent out=Component.empty();Matcher matcher=TOKEN.matcher(raw);
        Style style=Style.EMPTY;int start=0;
        while(matcher.find()){
            if(matcher.start()>start)out.append(Component.literal(raw.substring(start,matcher.start())).setStyle(style));
            if(matcher.group(1)!=null)style=style.withColor(TextColor.fromRgb(Integer.parseInt(matcher.group(1),16)));
            else {
                char c=Character.toLowerCase(matcher.group(2).charAt(0));
                String codes="0123456789abcdef";
                int idx=codes.indexOf(c);
                if(idx>=0){int[] colors={0x000000,0x0000AA,0x00AA00,0x00AAAA,0xAA0000,0xAA00AA,0xFFAA00,0xAAAAAA,0x555555,0x5555FF,0x55FF55,0x55FFFF,0xFF5555,0xFF55FF,0xFFFF55,0xFFFFFF};style=Style.EMPTY.withColor(TextColor.fromRgb(colors[idx]));}
                else if(c=='l')style=style.withBold(true);
                else if(c=='o')style=style.withItalic(true);
                else if(c=='n')style=style.withUnderlined(true);
                else if(c=='m')style=style.withStrikethrough(true);
                else if(c=='k')style=style.withObfuscated(true);
                else if(c=='r')style=Style.EMPTY;
            }
            start=matcher.end();
        }
        if(start<raw.length())out.append(Component.literal(raw.substring(start)).setStyle(style));
        return out;
    }
}
