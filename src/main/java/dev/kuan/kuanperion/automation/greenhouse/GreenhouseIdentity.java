// SPDX-License-Identifier: GPL-3.0-only
// Mature-crop texture hashes adapted from supplied Aether modules/GreenhouseManager; local strict parser reused.
package dev.kuan.kuanperion.automation.greenhouse;
import dev.kuan.kuanperion.automation.TextureIdentity;import java.util.*;
/** Texture names alone never authorize a harvest; world/plot/tool/target permissions are separate. */
public final class GreenhouseIdentity {
 private GreenhouseIdentity(){}
 public enum Kind {ASHWREATH,TURTELLINI,GLASSCORN}
 private static final Map<String,Kind> HASHES=Map.of(
  "5890f50780fdecedaa85aa40bf3399e9439ee68594c6d022688165608171681d",Kind.ASHWREATH,
  "1d1bd06a6738d0da5053eae49a1362b89489d1ac004c222504536f7bcd07679d",Kind.TURTELLINI,
  "297de27338b9f876e570d1cc01fe1beccfc940467c5c97c467e93e79c81c25ee",Kind.GLASSCORN);
 public static Optional<Kind> kindHash(String hash){return Optional.ofNullable(hash).map(HASHES::get);}
 public static Optional<Kind> kind(String encoded){return TextureIdentity.hash(encoded).map(HASHES::get);}
}
