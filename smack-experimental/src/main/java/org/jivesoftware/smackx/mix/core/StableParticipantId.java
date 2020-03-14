package org.jivesoftware.smackx.mix.core;

import java.util.stream.IntStream;

import org.jivesoftware.smack.util.StringUtils;

public final class StableParticipantId implements CharSequence {

    private final String id;

    private StableParticipantId(String string) {
        String id = StringUtils.requireNotNullNorEmpty(string.trim(),
                "Stable Participant ID MUST NOT be null, nor empty.");
        if (id.contains("#") || id.contains("/") || id.contains("@")) {
            throw new IllegalArgumentException("Stable Participant ID MUST NOT contain the '#', '/' or '@' characters.");
        }
        this.id = id;
    }

    @Override
    public int length() {
        return id.length();
    }

    @Override
    public char charAt(int i) {
        return id.charAt(i);
    }

    @Override
    public CharSequence subSequence(int i, int i1) {
        return id.subSequence(i, i1);
    }

    @Override
    public IntStream chars() {
        return id.chars();
    }

    @Override
    public IntStream codePoints() {
        return id.codePoints();
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof StableParticipantId)) {
            return false;
        }
        return toString().equals(((StableParticipantId) obj).toString());
    }

    @Override
    public String toString() {
        return id;
    }
}
