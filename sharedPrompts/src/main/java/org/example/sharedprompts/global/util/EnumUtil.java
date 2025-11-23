package org.example.sharedprompts.global.util;

public class EnumUtil {
    private EnumUtil() {}

    public static <E extends Enum<E> & LabelEnum> String getLabel(E e) {
        return e == null ? "" : e.getLabel();
    }

    public interface LabelEnum {
        String getLabel();
    }
}

