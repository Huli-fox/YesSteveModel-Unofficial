# Refactor First-Person Empty-Hand Rendering

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds.

This document must be maintained in accordance with `.agent/PLANS.md` from the repository root. It is self-contained: a contributor should be able to start from this file and the current working tree, without relying on prior chat history.

## Purpose / Big Picture

Yes Steve Model replaces the player model with custom Geckolib geometry, and the same replacement should feel natural in first person. The current empty-hand renderer cancels Forge's hand event and hand-builds its own projection, swing, equip, and lighting path in `ClientEventHandler`. In game this makes the empty right arm sit and light differently from the vanilla Minecraft 1.7.10 arm. After this change, custom empty-hand rendering should reuse the same camera setup, arm smoothing, swing/equip constants, lightmap coordinates, and overlay behavior used by vanilla `ItemRenderer.renderItemInFirstPerson`, while still drawing the selected YSMU arm model and texture.

The result is visible by starting a client, selecting a YSMU model, switching to first person with an empty main hand, and comparing the arm while idling, walking, swinging, entering water, standing in fire, and moving between bright and dark blocks. The arm should follow vanilla-like placement and brightness instead of the previous hand-tuned placement.

## Progress

- [x] (2026-05-02 16:45 +08:00) Read `.agent/PLANS.md`, `ClientEventHandler`, `CustomPlayerRenderer`, `CustomPlayerModel`, Geckolib render helpers, and the generated Minecraft 1.7.10 `ItemRenderer` and `EntityRenderer` sources.
- [x] (2026-05-02 16:45 +08:00) Identified that the old custom path cancels `RenderHandEvent` before all resources are validated, rebuilds hand projection with fixed FOV `70.0F`, omits the vanilla render-arm pitch/yaw smoothing, does not set lightmap texture coordinates, and does not render first-person overlays after cancellation.
- [x] (2026-05-02 16:50 +08:00) Extracted first-person custom empty-hand rendering into `FirstPersonHandRenderer`; `ClientEventHandler` now only performs event eligibility, model lookup, event posting, and delegation.
- [x] (2026-05-02 16:50 +08:00) Replaced the old fixed-FOV/custom swing path with vanilla first-person camera setup, render-arm pitch/yaw smoothing, lightmap setup, and vanilla empty-hand swing/equip constants.
- [x] (2026-05-02 16:50 +08:00) Preserved Backhand offhand rendering after the custom main hand and restored custom blend/cull state before handing control to Backhand.
- [x] (2026-05-02 16:50 +08:00) Ran source-level checks: old hand helper search has no matches in `ClientEventHandler`, `git diff --check` produced no output, and the new untracked files have no trailing whitespace.
- [x] (2026-05-02 17:01 +08:00) Corrected the arm-local direction mismatch by flipping the Geckolib arm on its length axis before it enters vanilla first-person arm space.
- [x] (2026-05-02 17:01 +08:00) Re-ran `git diff --check` and trailing-whitespace checks after the direction fix; both produced no output.
- [x] (2026-05-02 17:09 +08:00) Added a 180 degree roll around the Geckolib arm length axis so the palm/back orientation matches the first-person view.
- [x] (2026-05-02 17:51 +08:00) Changed the 180 degree roll from origin-based rotation to pivoted rotation around the rendered arm's computed X/Z center line, preserving the pre-roll screen position.
- [x] (2026-05-02 17:51 +08:00) Re-ran `git diff --check`, trailing-whitespace checks, and source search for the pivoted roll helpers; checks produced no whitespace output.
- [ ] User should run `.\gradlew.bat build`, because this workspace's Gradle build/test commands are user-run.

## Surprises & Discoveries

- Observation: Forge posts `RenderHandEvent` before vanilla clears the depth buffer and before `EntityRenderer.renderHand` sets up the hand projection.
  Evidence: `build/rfg/minecraft-src/java/net/minecraft/client/renderer/EntityRenderer.java` lines 1434-1437 call `ForgeHooksClient.renderFirstPersonHand(...)`, then `GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT)`, then `this.renderHand(...)` only when the event is not canceled.

- Observation: Vanilla empty-hand rendering does more than swing the arm. It first rotates standard item lighting by interpolated player pitch/yaw, applies `EntityPlayerSP.renderArmPitch/renderArmYaw` smoothing, sets the world lightmap texture coordinates, and only then applies the empty-hand swing/equip transforms.
  Evidence: `ItemRenderer.renderItemInFirstPerson(float)` sets `RenderHelper.enableStandardItemLighting()`, rotates by render-arm deltas, calls `OpenGlHelper.setLightmapTextureCoords(...)`, and in the `itemstack == null` branch applies the constants `0.8F`, `0.8F * f13`, `-0.75F * f13`, `-0.9F * f13`, `45.0F`, `70.0F`, and `-20.0F`.

- Observation: The old YSMU handler cancels the event before checking the selected model EEP, arm model, renderer, or cached custom player. If any of those are unavailable, vanilla hand rendering is also suppressed.
  Evidence: `ClientEventHandler.onRenderHand` calls `event.setCanceled(true)` immediately after `shouldRenderCustomHand(...)`, then returns early on several missing-resource branches.

- Observation: Access Transformer wildcard lines need both field and method coverage for `EntityRenderer`.
  Evidence: `ysmu_at.cfg` already used `ItemRenderer *` for fields, while `geckolib_at.cfg` uses `RenderHelper *()` for methods. The final `ysmu_at.cfg` therefore contains both `public net.minecraft.client.renderer.EntityRenderer *` and `public net.minecraft.client.renderer.EntityRenderer *()`.

- Observation: YSMU arm models point opposite to vanilla `ModelBiped.bipedRightArm` along the arm length. The screen-facing end after the first implementation was the model's shoulder, not the hand.
  Evidence: Built-in arm JSON has hand/forearm cubes at lower Bedrock Y coordinates, such as `RightHandLocator` near Y `17`, while shoulder/upper-arm cubes are near Y `29`. Vanilla's right arm local space renders from shoulder near Y `0` toward the hand at larger Y after `ModelBiped.bipedRightArm.render(0.0625F)`.

- Observation: After the endpoint direction was corrected, the arm still needed a roll around the shoulder-to-hand axis. Visually this is the difference between showing the palm side and the back-of-hand side.
  Evidence: Runtime feedback after the Y-axis flip reported that the hand endpoint was correct, but the hand surface orientation needed a 180 degree rotation around the shoulder-hand line.

- Observation: Rotating 180 degrees around local Y at the model origin changes the arm's screen position, because the arm mesh is offset from that origin in X/Z.
  Evidence: Runtime screenshots after the origin-based roll showed the hand orientation was correct, but the arm shifted noticeably. The built-in Steve-style right arm is centered around X `2 / 16` and Z `0`, not X `0`.

## Decision Log

- Decision: Add a dedicated `FirstPersonHandRenderer` under `src/main/java/com/fox/ysmu/client/renderer/` instead of keeping the OpenGL pipeline inside `ClientEventHandler`.
  Rationale: `ClientEventHandler` is an event hub. Keeping projection setup, lightmap setup, vanilla empty-hand transforms, Geckolib bone drawing, and overlay restoration in a renderer-specific class makes the state transitions easier to audit.
  Date/Author: 2026-05-02 / Codex

- Decision: Expose `EntityRenderer` through the existing YSMU Access Transformer file, mirroring the project's existing `ItemRenderer *` exposure.
  Rationale: The closest vanilla behavior comes from calling the exact 1.7.10 methods `getFOVModifier`, `hurtCameraEffect`, and `setupViewBobbing`, and from reading the same camera zoom/far-plane state. Duplicating those private methods would make the mod drift from Forge/Minecraft behavior.
  Date/Author: 2026-05-02 / Codex

- Decision: Render only when all custom hand resources are ready; otherwise leave the Forge event uncanceled so vanilla can draw its normal arm or item path.
  Rationale: Missing custom model data should degrade to Minecraft's default behavior rather than producing no hand at all.
  Date/Author: 2026-05-02 / Codex

- Decision: Check that the arm model contains a renderable `RightArm` bone before canceling `RenderHandEvent`.
  Rationale: A malformed or unusual arm model should fall back to vanilla hand rendering instead of canceling the hand event and drawing nothing.
  Date/Author: 2026-05-02 / Codex

- Decision: Fix the direction mismatch with a local `GL11.glScalef(1.0F, -1.0F, 1.0F)` in `alignGeckoArmToVanillaBipedArm`, after translating the YSMU shoulder area to vanilla arm origin.
  Rationale: The vanilla first-person camera and swing/equip transforms were already accepted as close to original behavior. Flipping only the Geckolib arm-local Y axis preserves those vanilla transforms while swapping the visible endpoint from shoulder to hand.
  Date/Author: 2026-05-02 / Codex

- Decision: Add `GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F)` after the local Y-axis flip.
  Rationale: In the arm alignment space, local Y is the shoulder-to-hand length axis. A 180 degree rotation around that axis changes palm/back orientation without changing the accepted endpoint, camera, lighting, or swing behavior.
  Date/Author: 2026-05-02 / Codex

- Decision: Compute the roll pivot from the `RightArm` subtree's X/Z vertex bounds and rotate around that center line with `translate(pivot) -> rotate -> translate(-pivot)`.
  Rationale: A centered pivot keeps the arm's bounding box in the same screen position after the 180 degree roll. Computing it from model geometry supports Steve, Alex, and custom arm widths better than hard-coding a single X offset.
  Date/Author: 2026-05-02 / Codex

## Outcomes & Retrospective

Source implementation is complete. `ClientEventHandler` no longer owns hand projection, lightmap, swing, or arm placement logic. `FirstPersonHandRenderer` now replays vanilla's hand-phase camera, lighting, empty-hand transform, overlay, and Backhand offhand flow while replacing only the final right-arm model draw. After runtime feedback, the Geckolib arm-local Y axis is flipped so the hand end, not the shoulder end, appears in first person, and the arm is rolled 180 degrees around a computed center line so the hand surface faces correctly without shifting from the pre-roll screen position. Build verification remains pending user execution.

## Context and Orientation

The repository root is `D:\Code\YesSteveModel-Unofficial`. The mod id is `ysmu`, and this work touches only client-side first-person rendering for Minecraft Forge 1.7.10.

`src/main/java/com/fox/ysmu/client/ClientEventHandler.java` subscribes to Forge client events. Its `onRenderHand(RenderHandEvent event)` method currently decides whether to replace the first-person hand, cancels the event, looks up the selected model, and renders the custom arm directly.

`RenderHandEvent` is Forge's cancelable event for the first-person hand phase. In this Forge 1.7.10 source tree, the event is posted before vanilla clears the depth buffer and before vanilla calls `EntityRenderer.renderHand`. If this event is canceled, YSMU must render the whole hand phase it needs, including first-person overlays such as fire, water, or inside-block overlays.

`src/main/resources/META-INF/ysmu_at.cfg` is an Access Transformer file. An Access Transformer changes private Minecraft members to public in the development and runtime classpath. This project already uses it to expose all members of `net.minecraft.client.renderer.ItemRenderer`, which is why `ClientEventHandler` can read `itemRenderer.itemToRender` and equip progress fields. This plan extends the same mechanism to `EntityRenderer` for the exact vanilla hand camera helpers.

`src/main/java/com/fox/ysmu/client/renderer/CustomPlayerRenderer.java` is the Geckolib renderer used for third-person custom players. It provides `renderRecursively(...)`, which can draw a single `GeoBone` and its child bones. The first-person hand path should keep using this renderer for geometry, but should not drive the whole third-person player render.

`software/bernie/geckolib3/geo/render/built/GeoModel.java` stores parsed model bones. `ModelIdUtil.getArmId(modelId)` turns a selected model id such as `ysmu:default` into an arm model id such as `ysmu:default/arm`. The first-person hand model is expected to contain a `RightArm` bone, matching the existing code.

## Plan of Work

First, create `src/main/java/com/fox/ysmu/client/renderer/FirstPersonHandRenderer.java`. This class should hold the vanilla first-person hand pipeline: check whether custom empty-hand rendering is eligible, set the hand projection and modelview matrices the same way vanilla `EntityRenderer.renderHand` does, apply hurt camera and view bobbing, enable the lightmap, render the custom main hand, call Backhand's offhand hook, disable the lightmap, and finally render overlays like vanilla does after the hand matrix pop.

Second, move the empty-hand item renderer logic into this helper. It should copy the vanilla 1.7.10 empty-hand branch constants from `ItemRenderer.renderItemInFirstPerson(float)`: first apply player arm pitch/yaw smoothing, set the lightmap from the player's current block position, translate by swing and equip progress, rotate by the same 45/70/-20 degree sequence, bind the YSMU texture, and draw the Geckolib `RightArm` bone.

Third, add a small alignment transform immediately before drawing the Geckolib bone. Minecraft's `ModelBiped.bipedRightArm.render(0.0625F)` renders a right arm whose local x range is roughly `-0.5` to `-0.25` and whose y range starts near `0` at the shoulder and extends toward the hand at larger y. YSMU arm JSON is Bedrock-style geometry converted by the embedded Geckolib builder, so the Steve-style right arm occupies about `0` to `0.25` on x, but its shoulder and hand endpoints are reversed relative to vanilla first-person space. Translate the Geckolib arm by about `-0.5` on x and `29 / 16` on y, then scale y by `-1`, so the upper-arm shoulder maps near vanilla y `0` and the hand end maps toward the screen-facing end expected by the vanilla transform. Finally compute the rendered `RightArm` subtree's X/Z center line and rotate 180 degrees around local y at that center, which corrects the palm/back roll without moving the arm's screen position.

Fourth, simplify `ClientEventHandler.onRenderHand`. It should gather `Minecraft`, player, `ItemRenderer`, selected model id, arm model, custom player entity, and `CustomPlayerRenderer`. It should post `SpecialPlayerRenderEvent` as before. Only after all of that succeeds should it cancel the Forge event and call `FirstPersonHandRenderer.render(...)`.

Fifth, update `src/main/resources/META-INF/ysmu_at.cfg` to expose `EntityRenderer` members. This is needed for exact FOV, hurt-camera, view-bobbing, camera zoom, and far-plane behavior.

## Concrete Steps

From repository root `D:\Code\YesSteveModel-Unofficial`, edit:

    src/main/java/com/fox/ysmu/client/renderer/FirstPersonHandRenderer.java
    src/main/java/com/fox/ysmu/client/ClientEventHandler.java
    src/main/resources/META-INF/ysmu_at.cfg

After source edits, run source-level checks from the repository root:

    rg -n "pushFirstPersonMatrices|applyRightArmPlacement|setupHandLighting|applySwingTransform|bobView" src/main/java/com/fox/ysmu/client/ClientEventHandler.java
    git diff --check
    Select-String -Path .agent/FIRST_PERSON_HAND_EXECPLAN.md,src/main/java/com/fox/ysmu/client/renderer/FirstPersonHandRenderer.java -Pattern '[ \t]+$'

The first command should produce no matches after the old hand-specific helper methods are removed. The second and third commands should produce no output.

Because this workspace's AGENTS instructions say Gradle builds/tests are user-run, ask the user to run:

    .\gradlew.bat build

## Validation and Acceptance

The source-level acceptance is that `ClientEventHandler` no longer owns the hand projection and arm placement implementation, the new renderer compiles against exposed `EntityRenderer` helpers, and `git diff --check` has no whitespace errors.

The build acceptance is that the user runs `.\gradlew.bat build` from `D:\Code\YesSteveModel-Unofficial` and reports success. If compilation fails because a Minecraft member name differs in the transformed source, update `ysmu_at.cfg` or the Java call site to match the generated source names under `build/rfg/minecraft-src/java`.

The manual runtime acceptance is to start `.\gradlew.bat runClient`, enter a world, select a YSMU model, switch to first person with an empty main hand, and observe that the custom right arm follows vanilla-like idle, walk bobbing, and swing placement. Move between sunlight and a dark area; the arm should brighten and darken like vanilla because lightmap coordinates are set. Stand in water, fire, or inside a block; the normal first-person overlay should still render because the canceled event now replays vanilla overlay rendering.

## Idempotence and Recovery

The edits are source-only and can be repeated safely. If the custom arm disappears, first leave `RenderHandEvent` uncanceled until all resources are validated; vanilla fallback should still draw a normal hand. If the arm is visible but shifted, adjust only the alignment constants in `FirstPersonHandRenderer`, not the vanilla swing/equip constants. If an OpenGL state leak appears, compare the helper's order with `ItemRenderer.renderItemInFirstPerson(float)` and `EntityRenderer.renderHand(float, int)` in the generated Minecraft source.

## Artifacts and Notes

Important source references used for this plan:

    build/rfg/minecraft-src/java/net/minecraft/client/renderer/EntityRenderer.java
    build/rfg/minecraft-src/java/net/minecraft/client/renderer/ItemRenderer.java
    build/rfg/minecraft-src/java/net/minecraft/client/renderer/entity/RenderPlayer.java
    src/main/java/com/fox/ysmu/client/ClientEventHandler.java

The vanilla empty-hand branch renders `RenderPlayer.renderFirstPersonArm(...)`. YSMU replaces that last call with `CustomPlayerRenderer.renderRecursively(...)` for the `RightArm` bone, after applying the alignment transform described above.

Source-level evidence after implementation:

    rg -n "pushFirstPersonMatrices|applyRightArmPlacement|setupHandLighting|applySwingTransform|bobView|GlStateManager|MathHelper|Quaternion|Axis|Utils|Project|Tessellator|RenderHelper|GL11" src/main/java/com/fox/ysmu/client/ClientEventHandler.java
    # no matches

    git diff --check
    # no output

    Select-String -Path .agent/FIRST_PERSON_HAND_EXECPLAN.md,src/main/java/com/fox/ysmu/client/renderer/FirstPersonHandRenderer.java -Pattern '[ \t]+$'
    # no output

    rg -n "ArmRollPivot|ArmRollBounds|collectArmRollBounds|findArmRollPivot|glRotatef\\(GECKO_ARM_ROLL_DEGREES|glTranslatef\\(rollPivot" src/main/java/com/fox/ysmu/client/renderer/FirstPersonHandRenderer.java
    # shows the pivoted local roll used to orient the hand end without shifting it

## Interfaces and Dependencies

`FirstPersonHandRenderer` must expose:

    public static boolean shouldRenderCustomHand(Minecraft mc, EntityPlayer player, ItemRenderer itemRenderer);
    public static boolean hasRenderableRightArm(GeoModel geoModel);
    public static void render(RenderHandEvent event, Minecraft mc, EntityPlayer player, ItemRenderer itemRenderer, CustomPlayerRenderer renderer, GeoModel geoModel, CustomPlayerEntity customPlayer);

`ClientEventHandler.onRenderHand` must call `event.setCanceled(true)` only after the EEP, selected model id, arm model, renderer, cached custom player, and `SpecialPlayerRenderEvent` all allow the custom path.

`ysmu_at.cfg` must expose `net.minecraft.client.renderer.EntityRenderer` members so the renderer helper can call the same camera and view-effect methods that vanilla uses.

Revision note, 2026-05-02: Initial plan created before implementation to satisfy the repository ExecPlan requirement for a significant rendering refactor.

Revision note, 2026-05-02: Updated after implementation to record the extracted renderer, vanilla hand pipeline alignment, source-level checks, and remaining user-run Gradle build.

Revision note, 2026-05-02: Updated after runtime feedback that the arm model direction was reversed; the plan now records the local Y-axis flip and validation checks.

Revision note, 2026-05-02: Updated after runtime feedback that the palm/back orientation was reversed; the plan now records the 180 degree roll around the arm length axis.

Revision note, 2026-05-02: Updated after runtime feedback that origin-based roll shifted the arm; the plan now records the computed center-line pivot and validation checks.
