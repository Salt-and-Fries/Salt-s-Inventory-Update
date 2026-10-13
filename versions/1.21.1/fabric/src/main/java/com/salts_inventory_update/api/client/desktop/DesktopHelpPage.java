package com.salts_inventory_update.api.client.desktop;

import java.util.List;
import java.util.Objects;

/** Plain-text integration guidance displayed in the core instructions window. */
public record DesktopHelpPage(String title, String iconText, List<Section> sections) {
    public DesktopHelpPage { Objects.requireNonNull(title); Objects.requireNonNull(iconText); sections = List.copyOf(sections); }
    public record Section(String title, List<String> lines) {
        public Section { Objects.requireNonNull(title); lines = List.copyOf(lines); }
    }
}
