// SPDX-License-Identifier: GPL-3.0-only
// Curated texture hashes adapted from supplied Aether PestTargetTracker; strict validation is local.
package dev.kuan.kuanperion.automation.pest;

import dev.kuan.kuanperion.automation.ItemSnapshot;
import java.util.*;
import java.util.regex.Pattern;

/** Offline identity checks only. Texture URLs are parsed, NEVER fetched or resolved. */
public final class PestIdentity {
    private PestIdentity(){}
    private static final Set<String> TEXTURES=Set.of(
        "70a1e836bf1968b2eaa4837227a19204f17295d870ee9e754bd6b6d60ddbed3c",
        "a24c69f96ce5562221e195c8ef2bfad71ebf7f95f5ae914a484a8d0ec21672674",
        "6403ba4027a333d8d2fd32ab59d1cfdbaa7d908d80d2381db2a69cbe65450ad8",
        "9d90e777826a52461368e26d1b2e19bfa1ba582d60248e545f4124d0f731842",
        "4b24a482a32db1ea78fb98060b0c2fa4a373cbd18a68edddeb7419455a59cda9",
        "be6baf6431a9daa2ca604d5a3c26e9a761d5952f0817174a4fe0b764616e21ff",
        "52a9fe05bc663efcd12e56a3ccc5ec035bf577b78708548b6f4ffcf1d30eccfe",
        "6545c4b34e5b5470be94de100e61f7816f81bc5a11dfdf0eccf890172da5d0a",
        "a8abb471db0ab78703011997dc8b40798a941f3a4dec3ec61cbeec2af8cffe8",
        "7a79d0fd677b54530961117ef84adc206e2cc5045c1344d61d776bf8ac2fe1ba",
        "1e04bb6367caa4e88f5fd0ee80f0745d137a604223dbbc42a16471fdf64bb83",
        "4ce69e90adf34718f313ec24d6c6135b69b3788c61849844666ccc83ca640c0b16",
        "254aff4c0b2dce3a672349cc0e99e6f3a9deebe4b3556e84611eca250a7821bf");
    // Official public item catalog, checked 2026-10-24; only category VACUUM, not other similarly named items.
    private static final Set<String> VACUUMS=Set.of("SKYMART_VACUUM","SKYMART_TURBO_VACUUM","SKYMART_HYPER_VACUUM","INFINI_VACUUM","INFINI_VACUUM_HOOVERIUS");
    private static final Pattern RANGE=Pattern.compile("\\bWITHIN\\s+([0-9]{1,2}(?:\\.[0-9]{1,2})?)\\s+BLOCKS\\b");
    public record Vacuum(int slot,ItemSnapshot item,double range){}
    public static Optional<Vacuum> vacuum(int slot,ItemSnapshot item){
        if(slot<0||slot>8||item==null||item.count()!=1||!VACUUMS.contains(item.skyblockId())||item.lore().size()>64||item.lore().stream().anyMatch(l->l.length()>1024))return Optional.empty();
        var lines=item.lore().stream().map(line->line.replaceAll("(?i)§[0-9a-fk-orx]","").toUpperCase(Locale.ROOT).replaceAll("\\s+"," ").strip()).toList();
        int start=-1,end=lines.size();
        for(int i=0;i<lines.size();i++){
            String line=lines.get(i);
            if(line.equals("ABILITY: VACUUM")||line.equals("ABILITY: VACUUM RIGHT CLICK")){if(start>=0)return Optional.empty();start=i;}
        }
        if(start<0)return Optional.empty();
        for(int i=start+1;i<lines.size();i++)if(lines.get(i).startsWith("ABILITY:")||lines.get(i).startsWith("VACUUM BAG:")){end=i;break;}
        String text=String.join(" ",lines.subList(start+1,end));
        var matcher=RANGE.matcher(text);if(!matcher.find())return Optional.empty();double range=Double.parseDouble(matcher.group(1));
        if(range<5||range>15||matcher.find())return Optional.empty();return Optional.of(new Vacuum(slot,item,range));
    }
    /** Usage counters/bag lore may change server-side; a nonempty UUID binds the actual tool. */
    public static boolean sameTool(ItemSnapshot expected,ItemSnapshot current){
        if(expected==null||current==null)return false;
        if(expected.uuid().isBlank())return expected.equals(current);
        return expected.count()==1&&current.count()==1&&expected.uuid().equals(current.uuid())&&expected.skyblockId().equals(current.skyblockId())&&expected.minecraftId().equals(current.minecraftId());
    }
    public static boolean sameVacuum(Vacuum expected,ItemSnapshot current){
        return expected!=null&&sameTool(expected.item(),current)&&vacuum(expected.slot(),current).filter(next->next.range()==expected.range()).isPresent();
    }
    public static boolean knownTexture(String encoded){return dev.kuan.kuanperion.automation.TextureIdentity.hash(encoded).filter(TEXTURES::contains).isPresent();}
}
