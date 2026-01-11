# PRD — SomAI (NeoForge 1.21.1)

## 1) Résumé
SomAI ajoute un compagnon humanoïde (IA “vivante”) contrôlable via un sifflet.
- **IA vivante** (suivi / idle / combat) = objectifs vanilla.
- **Mode automatisation** (goto/mine/follow/explore/farm/stop) = délégation à **PlayerEngine** (obligatoire), sans réinventer un système de tâches.

## 2) Objectifs produit
- Fournir un compagnon humanoïde persistant, possédable (owner/tame), avec inventaire.
- Centraliser le contrôle via un seul item (le sifflet), avec retours clairs dans le chat.
- Offrir une base solide d’actions PlayerEngine (API Automaton/Baritone) déclenchées par le sifflet.

## 3) Périmètre (scope)
### Inclus
- Entité compagnon humanoïde : apprivoisement, owner, comportement de suivi “wolf-like”, idle, combat automatique.
- Inventaire compagnon 27 slots + UI type chest.
- Item sifflet : link, summon/recall/dismiss, + modes d’automaton PlayerEngine.
- Persistance des données (whistle et compagnon).
- Outil dev : dump des classes PlayerEngine pour mapper précisément les features.

### Exclu / Non-objectifs
- Pas de framework custom de tâches/automation côté mod (tout passe par PlayerEngine).
- Pas de GUI dédiée avancée pour les modes (cycle simple via sneak + clic).
- Pas de système complet de “combat AI” PlayerEngine (on garde combat vanilla pour le compagnon “vivant”).

## 4) Plateforme & dépendances
- **Minecraft**: 1.21.1
- **Loader**: NeoForge
- **Java**: 21
- **Dépendance obligatoire**: PlayerEngine (CurseMaven `playerengine-1322604:7371353`)
- **Compat runtime**: Architectury NeoForge (pin)

Note: le dossier `libs/` contient actuellement `playerengine-neoforge-1.21.1-1.0.0.jar`. À vérifier côté licence/commit (souvent les jars ne doivent pas être commitées).

## 5) Comportement compagnon (IA “vivante”)
### Propriété / apprivoisement
- À la création via sifflet : le compagnon est **tame** et **owned** par le joueur.
- Ownership vérifié sur toutes les actions sensibles (open inventory, dismiss, automaton).

### Suivi / Idle
- Objectif: ne pas être collé au joueur.
- Implémentation: `FollowOwnerGoal` avec une **bande idle**:
  - Distance min: **10 blocs**
  - Distance max: **20 blocs**

### Wait / Follow (toggle)
- Le flag vanilla `orderedToSit` est réutilisé comme **WAIT**.
- Particularité: le compagnon **reste debout** (pas de pose assise), mais stop sa navigation.

### Combat automatique
- Objectifs vanilla :
  - Défend le owner (owner hurt by/target)
  - Riposte (hurt by)
  - Attaque des hostiles proches (Monster)
- Attributs clés:
  - `ATTACK_DAMAGE` présent.

## 6) Inventaire & UI
- **Taille**: 27 slots.
- **UI**: `ChestMenu.threeRows`.
- **Interactions**:
  - Clic droit sur compagnon: ouvre inventaire (si owner).
  - Sneak + clic droit sur compagnon: toggle WAIT/FOLLOW.

### PlayerEngine inventory/interaction manager
- Le compagnon implémente:
  - `IInventoryProvider` → `LivingEntityInventory`
  - `IInteractionManagerProvider` → `LivingEntityInteractionManager`
- Objectif: permettre aux features PlayerEngine d’utiliser le corps du compagnon comme “automatone”.

## 7) Sifflet (Whistle) — UX & contrôles
Le sifflet est la **surface de contrôle unique**.

### Link (associer le sifflet à un compagnon)
- Action: clic droit avec le sifflet en visant son compagnon (distance ~6 blocs).
- Résultat: stocke l’UUID du compagnon dans le sifflet.

### Summon / Recall (action par défaut)
- Clic droit normal:
  - Si compagnon lié et vivant:
    - Autre dimension → **téléport** sur le joueur.
    - Même dimension:
      - > 50 blocs → **téléport**.
      - ≤ 50 blocs → navigation vers joueur (pathfinding).
    - Le recall force `WAIT=false`.
  - Si pas de compagnon lié → **spawn** un nouveau compagnon près du joueur, puis link.

### Dismiss
- Sneak + clic droit **main hand**: cycle de mode automaton.
- Sneak + clic droit **off hand**: dismiss (discard) du compagnon lié + reset link.

## 8) Modes “Automaton” (PlayerEngine)
Les actions automaton sont déclenchées via le sifflet et déléguées à l’API Automaton de PlayerEngine (Baritone).

### Cycle de modes
`OFF → GOTO → MINE → FOLLOW → EXPLORE → FARM → STOP → OFF`

### Détails des modes
- **GOTO**
  - Raytrace bloc jusqu’à ~96 blocs.
  - `ICustomGoalProcess.setGoalAndPath(new GoalBlock(x,y,z))`

- **MINE**
  - Raytrace bloc.
  - `IMineProcess.mine(1, block)`

- **FOLLOW**
  - `IFollowProcess.follow(predicate)` avec predicate = entité ciblée = owner.

- **EXPLORE**
  - `IExploreProcess.explore(64, 256)`

- **FARM**
  - Raytrace bloc (centre).
  - `IFarmProcess.farm(48, center)` sinon `farm(48)`

- **STOP**
  - Annule pathing/mine/follow via `cancelEverything()` + `cancel()`.

## 9) Données & persistance
### Dans le sifflet (ItemStack components)
Stocké via `DataComponents.CUSTOM_DATA`:
- `SomAICompanionUuid` (UUID)
- `SomAIAutomatonMode` (string enum)

### Dans le compagnon (NBT)
- Inventaire 27 slots sous `Inventory`.
- Données PlayerEngine sous `PlayerEngineInventory` (tag dédié).

## 10) Chat / messages
- Tous les retours utilisateur passent par `SomAIChat` avec préfixe.
- Messages actuels en anglais (standardisation).

## 11) Implémentation — fichiers principaux
- `src/main/java/com/somapps/somai/HumanCompanionEntity.java`
  - AI goals (follow/idle/combat), wait/follow toggle, inventory + UI, persistance.
- `src/main/java/com/somapps/somai/CompanionWhistleItem.java`
  - Link/summon/recall/dismiss + modes automaton.
- `src/main/java/com/somapps/somai/PlayerEngineBridge.java`
  - Adaptateur direct API Automaton (BaritoneAPI/IBaritone) : goto/mine/follow/explore/farm/stop.
- `src/main/java/com/somapps/somai/SomAIChat.java`
  - Helper chat.

## 12) PlayerEngine — inventaire des commandes détectées (jar 7371353)
Ces classes existent dans `com.player2.playerengine.commands.*` (utile pour roadmap/extension des modes) :
- `GotoCommand`, `FollowCommand`, `FarmCommand`, `FishCommand`, `BuildStructureCommand`
- `AttackPlayerOrMobCommand`
- `DepositCommand`, `StashCommand`, `PickupDropsCommand`
- `PauseCommand`, `UnPauseCommand`, `StopCommand`, `StatusCommand`
- `InventoryCommand`, `EquipCommand`, `EatFoodCommand`, `FoodCommand`, `MeatCommand`
- `LocateStructureCommand`, `ResetMemoryCommand`, `ReloadSettingsCommand`
- `SetHostileAttackCommand`, `SetAIBridgeEnabledCommand`

## 13) Critères d’acceptation (MVP)
- Le sifflet peut link un compagnon en le regardant.
- Clic droit: summon si pas lié, sinon recall (pathfind proche / teleport loin / cross-dimension).
- Sneak + clic sur compagnon: toggle WAIT/FOLLOW (debout, pas assis).
- Inventaire 27 slots s’ouvre et persiste.
- Combat auto fonctionne contre hostiles proches et en défense du owner.
- Modes automaton:
  - GOTO/MINE/FOLLOW/EXPLORE/FARM/STOP déclenchent une action PlayerEngine sans crash.

## 14) Outil dev: dumpPlayerEngineApi
- Tâche Gradle: `dumpPlayerEngineApi`
- But: lister classes PlayerEngine (Automaton API / Commands / Tasks / Control) pour mapper les fonctionnalités.
- Input jar:
  - Priorité 1: `-PplayerEngineJar="C:\path\to\playerengine.jar"`
  - Priorité 2: `libs/playerengine-dev.jar`
  - Priorité 3: auto-détection dans le cache Gradle (coords pin).

## 15) Roadmap (propositions)
- Ajouter un mode **BUILD** (basé sur `IBuilderProcess.buildOpenSchematic()` + pause/resume).
- Ajouter **PAUSE/UNPAUSE** (PlayerEngine) distinct de STOP.
- Ajouter un mode **PICKUP/DEPOSIT/STASH** (via commandes PlayerEngine, si on décide d’utiliser `PlayerEngineController` ou un système de commande côté PlayerEngine).
- Localisation FR/EN via `Component.translatable`.
