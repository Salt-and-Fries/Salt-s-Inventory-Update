(function () {
  "use strict";

  const chapters = Array.from(document.querySelectorAll(".chapter"));
  const nav = document.getElementById("chapter-nav");
  const searchInput = document.getElementById("guide-search");
  const searchStatus = document.getElementById("search-status");
  const noResults = document.getElementById("no-results");
  const navLinks = new Map();

  function slug(text) {
    return text
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, "-")
      .replace(/^-|-$/g, "");
  }

  function normalize(text) {
    return (text || "").toLowerCase().replace(/\s+/g, " ").trim();
  }

  function ensureId(element, prefix) {
    if (!element.id) {
      element.id = `${prefix}-${slug(element.textContent)}`;
    }
    return element.id;
  }

  function buildApiParts() {
    chapters.forEach((chapter) => {
      const api = (chapter.dataset.api || "")
        .split(",")
        .map((part) => part.trim())
        .filter(Boolean);
      const holder = chapter.querySelector(".api-parts");
      if (!holder || api.length === 0) {
        return;
      }

      const label = document.createElement("span");
      label.className = "api-chip";
      label.textContent = "API parts";
      label.setAttribute("aria-hidden", "true");
      holder.appendChild(label);

      api.forEach((part) => {
        const button = document.createElement("button");
        button.type = "button";
        button.className = "api-chip";
        button.textContent = part;
        button.addEventListener("click", () => {
          searchInput.value = part;
          applySearch();
          searchInput.focus();
        });
        holder.appendChild(button);
      });
    });
  }

  function buildNavigation() {
    chapters.forEach((chapter, chapterIndex) => {
      const title = chapter.dataset.title || chapter.querySelector("h2").textContent.trim();
      const chapterId = ensureId(chapter, `chapter-${chapterIndex + 1}`);
      const group = document.createElement("div");
      group.className = "nav-chapter";

      const chapterLink = document.createElement("a");
      chapterLink.href = `#${chapterId}`;
      chapterLink.textContent = title;
      group.appendChild(chapterLink);
      navLinks.set(chapterId, chapterLink);

      chapter.querySelectorAll(".guide-section h3").forEach((heading, sectionIndex) => {
        const section = heading.closest(".guide-section");
        const sectionId = ensureId(section, `${chapterId}-section-${sectionIndex + 1}`);
        const sectionLink = document.createElement("a");
        sectionLink.href = `#${sectionId}`;
        sectionLink.className = "nav-section";
        sectionLink.textContent = heading.textContent.trim();
        group.appendChild(sectionLink);
        navLinks.set(sectionId, sectionLink);
      });

      nav.appendChild(group);
    });
  }

  function wrapCodeBlocks() {
    document.querySelectorAll("pre").forEach((pre) => {
      if (pre.parentElement && pre.parentElement.classList.contains("code-wrap")) {
        return;
      }

      const wrapper = document.createElement("div");
      wrapper.className = "code-wrap";
      pre.parentNode.insertBefore(wrapper, pre);
      wrapper.appendChild(pre);

      const button = document.createElement("button");
      button.type = "button";
      button.className = "copy-button";
      button.textContent = "Copy";
      button.addEventListener("click", () => copyCode(button, pre));
      wrapper.appendChild(button);
    });
  }

  async function copyCode(button, pre) {
    const text = pre.textContent;
    try {
      if (navigator.clipboard && window.isSecureContext) {
        await navigator.clipboard.writeText(text);
      } else {
        const textArea = document.createElement("textarea");
        textArea.value = text;
        textArea.setAttribute("readonly", "");
        textArea.style.position = "fixed";
        textArea.style.left = "-9999px";
        document.body.appendChild(textArea);
        textArea.select();
        document.execCommand("copy");
        textArea.remove();
      }
      button.textContent = "Copied";
      setTimeout(() => {
        button.textContent = "Copy";
      }, 1400);
    } catch (error) {
      button.textContent = "Failed";
      setTimeout(() => {
        button.textContent = "Copy";
      }, 1400);
    }
  }

  function setupTabs() {
    document.querySelectorAll("[data-tabs]").forEach((tabs, tabGroupIndex) => {
      const buttons = Array.from(tabs.querySelectorAll(".tab-list button"));
      const panels = Array.from(tabs.querySelectorAll(".tab-panel"));
      const groupId = `tabs-${tabGroupIndex}`;

      buttons.forEach((button, index) => {
        const target = button.dataset.target;
        const panel = panels.find((candidate) => candidate.dataset.panel === target);
        const buttonId = `${groupId}-tab-${index}`;
        const panelId = `${groupId}-panel-${index}`;
        button.id = buttonId;
        button.setAttribute("role", "tab");
        button.setAttribute("aria-controls", panelId);
        button.setAttribute("aria-selected", index === 0 ? "true" : "false");
        if (panel) {
          panel.id = panelId;
          panel.setAttribute("role", "tabpanel");
          panel.setAttribute("aria-labelledby", buttonId);
          panel.hidden = index !== 0;
        }

        button.addEventListener("click", () => {
          buttons.forEach((other) => other.setAttribute("aria-selected", "false"));
          panels.forEach((otherPanel) => {
            otherPanel.hidden = true;
          });
          button.setAttribute("aria-selected", "true");
          if (panel) {
            panel.hidden = false;
          }
        });
      });
    });
  }

  function textMatches(element, query) {
    return normalize(element.textContent).includes(query);
  }

  function setHeadingMatches(root, query) {
    root.querySelectorAll("h2, h3").forEach((heading) => {
      heading.classList.toggle("heading-match", query.length > 0 && textMatches(heading, query));
    });
  }

  function applySearch() {
    const query = normalize(searchInput.value);
    let visibleChapters = 0;
    let visibleSections = 0;

    chapters.forEach((chapter) => {
      const sections = Array.from(chapter.querySelectorAll(".guide-section"));
      const chapterHeadingMatch = textMatches(chapter.querySelector(".chapter-heading"), query);
      let chapterHasVisibleSection = false;

      sections.forEach((section) => {
        const sectionVisible = !query || chapterHeadingMatch || textMatches(section, query);
        section.hidden = !sectionVisible;
        if (sectionVisible) {
          chapterHasVisibleSection = true;
          visibleSections += 1;
        }
      });

      const chapterVisible = !query || chapterHeadingMatch || chapterHasVisibleSection;
      chapter.hidden = !chapterVisible;
      if (chapterVisible) {
        visibleChapters += 1;
      }

      const chapterLink = navLinks.get(chapter.id);
      if (chapterLink) {
        chapterLink.closest(".nav-chapter").hidden = !chapterVisible;
      }

      sections.forEach((section) => {
        const link = navLinks.get(section.id);
        if (link) {
          link.hidden = section.hidden || chapter.hidden;
        }
      });

      setHeadingMatches(chapter, query);
    });

    noResults.hidden = visibleChapters !== 0;
    if (!query) {
      searchStatus.textContent = "";
    } else {
      searchStatus.textContent = visibleChapters === 0
        ? "No matches"
        : `${visibleChapters} chapter${visibleChapters === 1 ? "" : "s"}, ${visibleSections} section${visibleSections === 1 ? "" : "s"}`;
    }
  }

  function setupSearch() {
    searchInput.addEventListener("input", applySearch);
    window.addEventListener("keydown", (event) => {
      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === "k") {
        event.preventDefault();
        searchInput.focus();
        searchInput.select();
      }
      if (event.key === "Escape" && document.activeElement === searchInput) {
        searchInput.value = "";
        applySearch();
      }
    });
  }

  function setupActiveNavigation() {
    if (!("IntersectionObserver" in window)) {
      return;
    }

    const observed = [
      ...chapters,
      ...Array.from(document.querySelectorAll(".guide-section"))
    ];

    const observer = new IntersectionObserver((entries) => {
      const visible = entries
        .filter((entry) => entry.isIntersecting)
        .sort((a, b) => b.intersectionRatio - a.intersectionRatio)[0];

      if (!visible) {
        return;
      }

      navLinks.forEach((link) => link.classList.remove("active"));
      const link = navLinks.get(visible.target.id);
      if (link) {
        link.classList.add("active");
      }
    }, {
      rootMargin: "-15% 0px -70% 0px",
      threshold: [0.1, 0.4, 0.7]
    });

    observed.forEach((element) => observer.observe(element));
  }

  function setupDetailsHash() {
    document.querySelectorAll("details").forEach((details, index) => {
      if (!details.id) {
        details.id = `troubleshooting-${index + 1}`;
      }
    });
  }

  buildApiParts();
  buildNavigation();
  wrapCodeBlocks();
  setupTabs();
  setupSearch();
  setupDetailsHash();
  setupActiveNavigation();
})();
