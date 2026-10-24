// SPDX-License-Identifier: GPL-3.0-only
package dev.kuan.kuanperion.automation;
/** Narrow outbound allowlist; no command comes from incoming chat or arbitrary configuration. */
public enum AutomationCommand {
 PICKUP_STASH("pickupstash");
 private final String wire;AutomationCommand(String wire){this.wire=wire;}public String wire(){return wire;}
 @FunctionalInterface public interface Sender {boolean send(AutomationCommand command);}
}
