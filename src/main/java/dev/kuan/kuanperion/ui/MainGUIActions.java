// SPDX-License-Identifier: GPL-3.0-only
// Adapted from Aether 1.3.1-r6; see THIRD_PARTY_NOTICES.md. Modified for Kuanperion / Minecraft 26.2.
package dev.kuan.kuanperion.ui;

import dev.kuan.kuanperion.ui.MainGUI;
import dev.kuan.kuanperion.ui.MainGUIContext;
import dev.kuan.kuanperion.ui.MainGUIRegistry;
import dev.kuan.kuanperion.ui.settings.ModulesTab;
import dev.kuan.kuanperion.ui.settings.Setting;
import dev.kuan.kuanperion.ui.settings.SettingGroup;
import java.util.List;

final class MainGUIActions {
    private final MainGUI owner;
    private final MainGUIContext context;

    MainGUIActions(MainGUI owner, MainGUIContext context) {
        this.owner = owner;
        this.context = context;
    }

    void resetActiveContentScroll() {
        this.owner.setActiveScroll(0.0f, 0.0f);
        this.syncActiveBodyScrollState();
        this.owner.refreshContext();
    }

    void syncActiveBodyScrollState() {
        if (this.owner.getActiveMainTab() != 0 || this.owner.isShowingSearchResults()) {
            return;
        }
        if (this.owner.getActiveModuleSubTab() != null) {
            this.owner.setModuleDetailScroll(this.owner.getActiveScrollY(), this.owner.getActiveTargetScrollY());
        } else {
            this.owner.setModulesOverviewScroll(this.owner.getActiveScrollY(), this.owner.getActiveTargetScrollY());
        }
        this.owner.refreshContext();
    }

    void enterModuleDetail(ModulesTab.SubTab subTab) {
        this.owner.setModulesOverviewScroll(this.owner.getActiveScrollY(), this.owner.getActiveTargetScrollY());
        this.owner.setActiveMainTab(0);
        this.owner.setActiveFilterIndex(MainGUIRegistry.moduleFilterFor(subTab));
        this.owner.setActiveModuleSubTab(subTab);
        this.owner.setActiveCategoryIndex(0);
        this.owner.setActiveScroll(0.0f, 0.0f);
        this.owner.setModuleDetailScroll(0.0f, 0.0f);
        this.owner.refreshContext();
    }

    void exitModuleDetail() {
        this.owner.setModuleDetailScroll(this.owner.getActiveScrollY(), this.owner.getActiveTargetScrollY());
        this.owner.setActiveModuleSubTab(null);
        this.owner.setActiveCategoryIndex(0);
        this.owner.setActiveScroll(this.owner.getModulesOverviewScrollY(), this.owner.getModulesOverviewTargetScrollY());
        this.owner.refreshContext();
    }

    void switchMainTab(int mainTab) {
        if (this.owner.getActiveMainTab() == mainTab) {
            return;
        }
        this.owner.commitText();
        this.owner.commitColor();
        this.owner.closeOpenDropdown();
        this.owner.setActiveMainTab(mainTab);
        this.owner.setActiveSubtabIndex(0);
        this.owner.setActiveScroll(0.0f, 0.0f);
        this.owner.setProfileScroll(0.0f, 0.0f);
        this.clearSearch();
        this.owner.setActiveFilterIndex(mainTab == 0 ? 1 : 0);
        this.owner.setFilterBarInitialized(false);
        this.owner.setActiveModuleSubTab(null);
        this.owner.setActiveCategoryIndex(0);
        this.owner.refreshContext();
    }

    void clearSearch() {
        this.owner.setSearchMode(false);
        this.owner.setSearchQuery("");
        this.owner.setSearchScroll(0.0f, 0.0f);
        this.owner.clearInlineTextSelection();
        this.owner.refreshContext();
    }

    void openSearchResult(int mainTab, ModulesTab.SubTab subTab, SettingGroup group, Setting setting) {
        this.owner.commitText();
        this.owner.commitColor();
        this.owner.closeOpenDropdown();
        this.owner.setSearchMode(false);
        this.owner.setSearchQuery("");
        this.owner.setSearchScroll(0.0f, 0.0f);
        this.owner.clearInlineTextSelection();
        if (mainTab == 0) {
            this.openModuleSearchResult(subTab, group, setting);
        } else {
            this.openFlatSearchResult(mainTab, subTab, group, setting);
        }
        this.owner.refreshContext();
    }

    private void openModuleSearchResult(ModulesTab.SubTab subTab, SettingGroup group, Setting setting) {
        this.owner.setActiveMainTab(0);
        this.owner.setActiveFilterIndex(MainGUIRegistry.moduleFilterFor(subTab));
        this.owner.setActiveSubtabIndex(MainGUIActions.indexOf(MainGUIRegistry.MODULE_SUBTABS, subTab));
        this.owner.setActiveMainTab(0);
        this.owner.setActiveFilterIndex(MainGUIRegistry.moduleFilterFor(subTab));
        this.owner.setActiveModuleSubTab(subTab);
        this.owner.setActiveScroll(0.0f, 0.0f);
        int groupIndex = subTab.groups().indexOf(group);
        if (groupIndex >= 0) {
            this.owner.setActiveCategoryIndex(groupIndex + 1);
            this.owner.ensureSearchGroupVisible(group);
        } else {
            this.owner.setActiveCategoryIndex(0);
        }
        float targetScroll = this.moduleSettingScrollOffset(subTab, group, setting);
        this.owner.setActiveScroll(0.0f, targetScroll);
        this.owner.setModuleDetailScroll(0.0f, targetScroll);
    }

    private void openFlatSearchResult(int mainTab, ModulesTab.SubTab subTab, SettingGroup group, Setting setting) {
        this.owner.setActiveMainTab(mainTab);
        this.owner.setActiveFilterIndex(0);
        this.owner.setActiveSubtabIndex(MainGUIActions.indexOf(this.owner.flatSubtabsForCurrentFilter(), subTab));
        this.owner.setActiveModuleSubTab(null);
        this.owner.setActiveCategoryIndex(0);
        this.owner.ensureSearchGroupVisible(group);
        this.owner.setActiveScroll(0.0f, this.flatSettingScrollOffset(subTab, group, setting));
    }

    private float moduleSettingScrollOffset(ModulesTab.SubTab subTab, SettingGroup targetGroup, Setting targetSetting) {
        if (targetGroup == null) {
            return 0.0f;
        }
        float offset = 65.0f;
        if (this.owner.isPetTrackerSettingsGroup(targetGroup)) {
            return Math.max(0.0f, offset - this.owner.moduleSettingsHeight() / 3.0f);
        }
        for (Setting setting : targetGroup.getSettings()) {
            if (!setting.isVisible()) continue;
            if (setting == targetSetting) {
                return Math.max(0.0f, offset - this.owner.moduleSettingsHeight() / 3.0f);
            }
            offset += this.owner.settingHeightFor(setting, this.owner.moduleSettingsWidth());
        }
        return Math.max(0.0f, offset - this.owner.moduleSettingsHeight() / 3.0f);
    }

    private float flatSettingScrollOffset(ModulesTab.SubTab targetSubtab, SettingGroup targetGroup, Setting targetSetting) {
        float offset = 0.0f;
        float groupW = this.owner.contentW() - 40.0f;
        for (ModulesTab.SubTab subtab : this.owner.flatSubtabsForCurrentFilter()) {
            for (SettingGroup group : subtab.groups()) {
                offset += 46.0f;
                if (!this.owner.shouldShowChildren(group) || !group.hasSettings()) continue;
                offset += 5.0f;
                for (Setting setting : group.getSettings()) {
                    if (!setting.isVisible()) continue;
                    if (subtab == targetSubtab && group == targetGroup && setting == targetSetting) {
                        return Math.max(0.0f, offset - this.owner.contentScrollHeight() / 3.0f);
                    }
                    offset += this.owner.settingHeightFor(setting, groupW);
                }
                if (subtab == targetSubtab && group == targetGroup) {
                    return Math.max(0.0f, offset - this.owner.contentScrollHeight() / 3.0f);
                }
                offset += 4.0f;
            }
        }
        return 0.0f;
    }

    private static int indexOf(List<ModulesTab.SubTab> subtabs, ModulesTab.SubTab target) {
        for (int i = 0; i < subtabs.size(); ++i) {
            if (subtabs.get(i) != target) continue;
            return i;
        }
        return 0;
    }

    MainGUIContext context() {
        this.owner.refreshContext();
        return this.context;
    }
}
