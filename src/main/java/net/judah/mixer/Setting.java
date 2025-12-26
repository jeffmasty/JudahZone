package net.judah.mixer;

import java.util.ArrayList;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import net.judah.fx.Effect;

@Data @EqualsAndHashCode(callSuper=true) @RequiredArgsConstructor
public class Setting extends ArrayList<Integer> {

    private final String effectName;

    public Setting(Effect effect) {
        this.effectName = effect.getName();
        for (int i = 0; i < effect.getParamCount(); i++)
        add(effect.get(i));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(effectName).append("(");
        for (int i = 0; i < this.size(); i++) {
            sb.append(get(i));
            if (i < this.size() -1)
                sb.append("/");
        }
        sb.append(")");
        return sb.toString();
    }

}
