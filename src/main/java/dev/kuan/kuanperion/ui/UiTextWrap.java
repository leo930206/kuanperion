package dev.kuan.kuanperion.ui;
import java.util.ArrayList;
import java.util.List;
import java.util.function.ToDoubleFunction;
/** Break oversized first tokens too; CJK labels do not have ASCII word separators. */
final class UiTextWrap {
    private UiTextWrap() {}
    static List<String> wrap(String text,double width,ToDoubleFunction<String> measure) {
        if(text==null||text.isBlank())return List.of("");
        if(width<=0)return List.of(text);
        List<String> lines=new ArrayList<>();String current="";
        for(String word:text.trim().split("\\s+")) {
            String candidate=current.isEmpty()?word:current+" "+word;
            if(measure.applyAsDouble(candidate)<=width){current=candidate;continue;}
            if(!current.isEmpty())lines.add(current);
            current="";
            for(int codepoint:word.codePoints().toArray()) {
                String ch=new String(Character.toChars(codepoint));
                if(!current.isEmpty()&&measure.applyAsDouble(current+ch)>width){lines.add(current);current="";}
                current+=ch;
            }
        }
        if(!current.isEmpty())lines.add(current);
        return lines.isEmpty()?List.of(""):List.copyOf(lines);
    }
}
