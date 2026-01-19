package net.judah.channel;

import java.util.ArrayList;

import judahzone.api.FX;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@Data @EqualsAndHashCode(callSuper=true) @RequiredArgsConstructor
public class Setting extends ArrayList<Integer> {

    private final String effectName;

    public Setting(FX effect) {
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
