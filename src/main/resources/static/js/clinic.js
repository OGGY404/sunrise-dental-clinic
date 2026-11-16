/* ===========================================================================
   Sunrise Dental Clinic - small screen extras
   CIS6003 Advanced Programming (WRIT1)

   Nothing here is needed for the system to work. If this file does not load,
   every page still shows and every form still saves. It only adds the amber
   "now" line to today's diary.
   =========================================================================== */

(function () {
    "use strict";

    // Turn "09:30" into minutes after midnight (570), so times can be compared.
    function toMinutes(text) {
        var parts = text.split(":");
        return Number(parts[0]) * 60 + Number(parts[1]);
    }

    // Write a number with two digits, so 9 becomes "09".
    function twoDigits(number) {
        return (number < 10 ? "0" : "") + number;
    }

    // Put the "now" line between the last visit that has started and the next
    // one, and make the visits that have started look quieter.
    function placeNowLine(diary) {
        var line = diary.querySelector(".now-line");
        var entries = diary.querySelectorAll(".diary-entry");
        if (!line || entries.length === 0) {
            return;
        }

        var clock = new Date();
        var now = clock.getHours() * 60 + clock.getMinutes();
        var nextEntry = null;

        for (var i = 0; i < entries.length; i++) {
            var started = toMinutes(entries[i].getAttribute("data-time")) <= now;
            entries[i].classList.toggle("is-past", started);
            if (!started && nextEntry === null) {
                nextEntry = entries[i];
            }
        }

        // Before the next visit, or at the very end if every visit has started.
        diary.insertBefore(line, nextEntry);

        line.querySelector(".now-time").textContent =
            twoDigits(clock.getHours()) + ":" + twoDigits(clock.getMinutes());
        line.classList.add("is-placed");
    }

    // On a phone the menu scrolls sideways, so the current page's tab can be
    // out of sight. Scroll the menu (only the menu, not the page) to show it.
    function showCurrentTab() {
        var menu = document.querySelector(".topbar nav");
        var current = menu && menu.querySelector("[aria-current='page']");
        if (!current || menu.scrollWidth <= menu.clientWidth) {
            return;
        }
        // Where the tab starts inside the menu, then centre it.
        var tabStart = current.getBoundingClientRect().left
            - menu.getBoundingClientRect().left + menu.scrollLeft;
        menu.scrollLeft = tabStart - (menu.clientWidth - current.offsetWidth) / 2;
    }

    function start() {
        showCurrentTab();

        var diaries = document.querySelectorAll(".diary[data-live]");
        for (var i = 0; i < diaries.length; i++) {
            placeNowLine(diaries[i]);
        }

        // Move the line on once a minute while the page stays open at the desk.
        if (diaries.length > 0) {
            setInterval(function () {
                for (var j = 0; j < diaries.length; j++) {
                    placeNowLine(diaries[j]);
                }
            }, 60 * 1000);
        }
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", start);
    } else {
        start();
    }
})();
