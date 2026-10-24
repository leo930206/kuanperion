// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation;

import java.util.Set;
import static dev.kuan.kuanperion.automation.AutomationCoordinator.Resource.*;

/** Stable persisted IDs; declaring a feature does not make an unfinished workflow executable. */
public enum AutomationFeature {
 PEST("pest","Farming",true,Set.of(COMMAND,MOVEMENT,LOOK,HOTBAR,SCREEN)),
 GREENHOUSE("greenhouse","Farming",true,Set.of(COMMAND,MOVEMENT,LOOK,HOTBAR)),
 VISITOR("visitor","Farming",true,Set.of(COMMAND,MOVEMENT,LOOK,SCREEN)),
 COMPOSTER("composter","Farming",true,Set.of(COMMAND,MOVEMENT,LOOK,SCREEN)),
 AUTO_SELL("autoSell","Trade",false,Set.of(COMMAND,SCREEN)),
 BOOK_COMBINE("bookCombine","Trade",false,Set.of(COMMAND,SCREEN)),
 PET_SELL("petSell","Trade",false,Set.of(COMMAND,SCREEN)),
 JUNK("junk","Trade",true,Set.of(COMMAND,MOVEMENT,SCREEN)),
 STASH("stash","Trade",false,Set.of(COMMAND)),
 SUPERCRAFT("supercraft","Other",false,Set.of(COMMAND,SCREEN));
 private final String id,page;private final boolean garden;private final Set<AutomationCoordinator.Resource> resources;
 AutomationFeature(String id,String page,boolean garden,Set<AutomationCoordinator.Resource> resources){this.id=id;this.page=page;this.garden=garden;this.resources=Set.copyOf(resources);}
 public String id(){return id;}public String page(){return page;}public boolean requiresGarden(){return garden;}public Set<AutomationCoordinator.Resource> resources(){return resources;}
 /** Only implemented task drivers may be exposed as working automation. */
 public boolean implemented(){return this==STASH;}
}
