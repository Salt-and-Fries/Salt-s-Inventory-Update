package com.salts_inventory_update.controlify.model;

import java.text.Collator;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Alphabetical presentation index; recipe identities and individual angular positions remain intact. */
public final class AlphabeticalRecipeIndex {
    public record Entry<T>(T value, String displayName, String stableKey) {
        public Entry { Objects.requireNonNull(value); Objects.requireNonNull(displayName); Objects.requireNonNull(stableKey); }
    }
    public record Group(String letter, int firstIndex) { }
    public record Catalog<T>(List<Entry<T>> entries, List<Group> groups) {
        public Catalog { entries = List.copyOf(entries); groups = List.copyOf(groups); }
        public static <T> Catalog<T> empty() { return new Catalog<>(List.of(), List.of()); }
    }
    private AlphabeticalRecipeIndex() { }

    public static <T> Catalog<T> build(List<Entry<T>> recipes, String language) {
        Collator collator = Collator.getInstance(Locale.forLanguageTag(language.replace('_', '-')));
        collator.setStrength(Collator.SECONDARY);
        var ordered = new ArrayList<>(recipes);
        ordered.sort(Comparator.<Entry<T>, String>comparing(Entry::displayName, collator)
            .thenComparing(Entry::stableKey));
        var firstLetters = new LinkedHashMap<String, Integer>();
        for (int i = 0; i < ordered.size(); i++) {
            String letter = initialLetter(ordered.get(i).displayName());
            if (!letter.isEmpty()) firstLetters.putIfAbsent(letter, i);
        }
        var groups = firstLetters.entrySet().stream().map(entry -> new Group(entry.getKey(), entry.getValue())).toList();
        return new Catalog<>(ordered, groups);
    }

    /** Fold accented Latin initials to A-Z; all other names retain their complete recipe positions. */
    public static String initialLetter(String name) {
        String folded = Normalizer.normalize(name.strip(), Normalizer.Form.NFKD).toUpperCase(Locale.ROOT);
        if (folded.isEmpty()) return "";
        int initial = folded.codePointAt(0);
        return initial >= 'A' && initial <= 'Z' ? Character.toString(initial) : "";
    }
}
