// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation;
import com.google.gson.*;import com.google.gson.stream.*;
import java.io.*;import java.net.URI;import java.nio.*;import java.nio.charset.*;import java.util.*;
/** Strict offline texture property projection; callers must still check their curated identity allowlist. Never resolves/fetches a skin. */
public final class TextureIdentity {
 private TextureIdentity(){}
    public static Optional<String> hash(String encoded){
        if(encoded==null||encoded.isEmpty()||encoded.length()>8192)return Optional.empty();
        try{
            byte[] bytes=Base64.getDecoder().decode(encoded);if(bytes.length>6144)return Optional.empty();
            String text=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
            var reader=new JsonReader(new StringReader(text));reader.setStrictness(Strictness.STRICT);
            JsonObject root=read(reader,0).getAsJsonObject();if(reader.peek()!=JsonToken.END_DOCUMENT)return Optional.empty();
            var url=root.getAsJsonObject("textures").getAsJsonObject("SKIN").get("url");if(!url.isJsonPrimitive()||!url.getAsJsonPrimitive().isString())return Optional.empty();
            var uri=new URI(url.getAsString());if(!Set.of("http","https").contains(uri.getScheme())||!"textures.minecraft.net".equals(uri.getHost())||uri.getPort()!=-1||uri.getRawUserInfo()!=null||uri.getRawQuery()!=null||uri.getRawFragment()!=null)return Optional.empty();
            String path=uri.getRawPath();if(path==null||!path.startsWith("/texture/"))return Optional.empty();String hash=path.substring(9);return hash.matches("[0-9a-f]{1,64}")?Optional.of(hash):Optional.empty();
        }catch(Exception malformed){return Optional.empty();}
    }
    /** Bounded strict JSON with duplicate-key rejection, including unknown fields. */
    private static JsonElement read(JsonReader reader,int depth)throws IOException{
        if(depth>8)throw new IOException("Deep texture property");
        return switch(reader.peek()){
            case BEGIN_OBJECT -> {var object=new JsonObject();reader.beginObject();int fields=0;while(reader.hasNext()){String key=reader.nextName();if(++fields>32||object.has(key))throw new IOException("Ambiguous texture property");object.add(key,read(reader,depth+1));}reader.endObject();yield object;}
            case BEGIN_ARRAY -> {var array=new JsonArray();reader.beginArray();while(reader.hasNext()){if(array.size()>=32)throw new IOException("Large texture property");array.add(read(reader,depth+1));}reader.endArray();yield array;}
            case STRING -> new JsonPrimitive(reader.nextString());
            case NUMBER -> new JsonPrimitive(new java.math.BigDecimal(reader.nextString()));
            case BOOLEAN -> new JsonPrimitive(reader.nextBoolean());
            case NULL -> {reader.nextNull();yield JsonNull.INSTANCE;}
            default -> throw new IOException("Invalid texture JSON");
        };
    }
}
