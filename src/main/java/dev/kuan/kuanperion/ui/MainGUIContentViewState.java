// SPDX-License-Identifier: GPL-3.0-only
// Adapted from Aether 1.3.1-r6; see THIRD_PARTY_NOTICES.md. Modified for Kuanperion / Minecraft 26.2.
package dev.kuan.kuanperion.ui;

import dev.kuan.kuanperion.ui.settings.ModulesTab;

record MainGUIContentViewState(int activeMain, int activeSubtab, int activeFilter, ModulesTab.SubTab activeSubTab, int activeCategoryIdx, String searchQuery) {
}
