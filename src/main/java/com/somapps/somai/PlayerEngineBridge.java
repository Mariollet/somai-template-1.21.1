package com.somapps.somai;

import java.util.function.Predicate;

import com.player2.playerengine.automaton.api.BaritoneAPI;
import com.player2.playerengine.automaton.api.IBaritone;
import com.player2.playerengine.automaton.api.behavior.IPathingBehavior;
import com.player2.playerengine.automaton.api.command.exception.CommandException;
import com.player2.playerengine.automaton.api.command.manager.ICommandManager;
import com.player2.playerengine.automaton.api.pathing.goals.GoalBlock;
import com.player2.playerengine.automaton.api.process.ICustomGoalProcess;
import com.player2.playerengine.automaton.api.process.IExploreProcess;
import com.player2.playerengine.automaton.api.process.IFarmProcess;
import com.player2.playerengine.automaton.api.process.IFollowProcess;
import com.player2.playerengine.automaton.api.process.IMineProcess;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;

/**
 * Thin adapter around PlayerEngine's Automaton (Baritone) API.
 *
 * PlayerEngine is a mandatory dependency for this mod, so we call the API directly.
 * This class does not re-implement tasks; it only delegates to PlayerEngine processes.
 */
public final class PlayerEngineBridge {
    private PlayerEngineBridge() {
    }

    public record AutomatonStatus(boolean available, boolean pathing, String goal) {
        public static AutomatonStatus unavailable() {
            return new AutomatonStatus(false, false, "");
        }
    }

    public static boolean isAvailable() {
        try {
            return BaritoneAPI.getProvider() != null;
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean stop(ServerPlayer owner, LivingEntity automatoneEntity) {
        try {
            IBaritone baritone = getBaritoneForEntity(automatoneEntity);
            if (baritone == null) {
                SomAIChat.error(owner, "PlayerEngine: no controller found for companion.");
                return false;
            }

            IPathingBehavior pathing = baritone.getPathingBehavior();
            if (pathing != null) {
                pathing.cancelEverything();
            }

            IMineProcess mine = baritone.getMineProcess();
            if (mine != null) {
                mine.cancel();
            }

            IFollowProcess follow = baritone.getFollowProcess();
            if (follow != null) {
                follow.cancel();
            }

            SomAIChat.action(owner, "Automaton stopped.");
            return true;
        } catch (Throwable t) {
            SomAIChat.error(owner, "PlayerEngine stop failed: " + t.getClass().getSimpleName());
            return false;
        }
    }

    public static boolean executeCommand(ServerPlayer owner, LivingEntity automatoneEntity, String command) {
        try {
            IBaritone baritone = getBaritoneForEntity(automatoneEntity);
            if (baritone == null) {
                SomAIChat.error(owner, "PlayerEngine: no controller found for companion.");
                return false;
            }

            ICommandManager mgr = baritone.getCommandManager();
            if (mgr == null) {
                SomAIChat.error(owner, "PlayerEngine: command manager not available.");
                return false;
            }

            try {
                boolean ok = mgr.execute(owner.createCommandSourceStack(), command);
                if (ok) {
                    SomAIChat.action(owner, "Automaton: command sent.");
                } else {
                    SomAIChat.warn(owner, "Automaton: command not accepted.");
                }
                return ok;
            } catch (CommandException e) {
                SomAIChat.error(owner, "Automaton command failed: " + e.getClass().getSimpleName());
                return false;
            }
        } catch (Throwable t) {
            SomAIChat.error(owner, "PlayerEngine command failed: " + t.getClass().getSimpleName());
            return false;
        }
    }

    public static boolean goToBlock(ServerPlayer owner, LivingEntity automatoneEntity, BlockPos pos) {
        try {
            IBaritone baritone = getBaritoneForEntity(automatoneEntity);
            if (baritone == null) {
                SomAIChat.error(owner, "PlayerEngine: no controller found for companion.");
                return false;
            }

            ICustomGoalProcess goalProcess = baritone.getCustomGoalProcess();
            if (goalProcess == null) {
                SomAIChat.error(owner, "PlayerEngine: custom goal process not available.");
                return false;
            }

            goalProcess.setGoalAndPath(new GoalBlock(pos.getX(), pos.getY(), pos.getZ()));
            SomAIChat.action(owner, "Automaton: going to target.");
            return true;
        } catch (Throwable t) {
            SomAIChat.error(owner, "PlayerEngine goto failed: " + t.getClass().getSimpleName());
            return false;
        }
    }

    public static boolean mineBlock(ServerPlayer owner, LivingEntity automatoneEntity, Block block) {
        try {
            IBaritone baritone = getBaritoneForEntity(automatoneEntity);
            if (baritone == null) {
                SomAIChat.error(owner, "PlayerEngine: no controller found for companion.");
                return false;
            }

            IMineProcess mine = baritone.getMineProcess();
            if (mine == null) {
                SomAIChat.error(owner, "PlayerEngine: mine process not available.");
                return false;
            }

            mine.mine(1, block);
            SomAIChat.action(owner, "Automaton: mining target block.");
            return true;
        } catch (Throwable t) {
            SomAIChat.error(owner, "PlayerEngine mine failed: " + t.getClass().getSimpleName());
            return false;
        }
    }

    public static boolean follow(ServerPlayer owner, LivingEntity automatoneEntity, Predicate<Entity> filter) {
        try {
            IBaritone baritone = getBaritoneForEntity(automatoneEntity);
            if (baritone == null) {
                SomAIChat.error(owner, "PlayerEngine: no controller found for companion.");
                return false;
            }

            IFollowProcess follow = baritone.getFollowProcess();
            if (follow == null) {
                SomAIChat.error(owner, "PlayerEngine: follow process not available.");
                return false;
            }

            follow.follow(filter);
            SomAIChat.action(owner, "Automaton: following.");
            return true;
        } catch (Throwable t) {
            SomAIChat.error(owner, "PlayerEngine follow failed: " + t.getClass().getSimpleName());
            return false;
        }
    }

    public static boolean explore(ServerPlayer owner, LivingEntity automatoneEntity, int minDistance, int maxDistance) {
        try {
            IBaritone baritone = getBaritoneForEntity(automatoneEntity);
            if (baritone == null) {
                SomAIChat.error(owner, "PlayerEngine: no controller found for companion.");
                return false;
            }

            IExploreProcess explore = baritone.getExploreProcess();
            if (explore == null) {
                SomAIChat.error(owner, "PlayerEngine: explore process not available.");
                return false;
            }

            explore.explore(minDistance, maxDistance);
            SomAIChat.action(owner, "Automaton: exploring.");
            return true;
        } catch (Throwable t) {
            SomAIChat.error(owner, "PlayerEngine explore failed: " + t.getClass().getSimpleName());
            return false;
        }
    }

    public static boolean farm(ServerPlayer owner, LivingEntity automatoneEntity, int range, BlockPos centerOrNull) {
        try {
            IBaritone baritone = getBaritoneForEntity(automatoneEntity);
            if (baritone == null) {
                SomAIChat.error(owner, "PlayerEngine: no controller found for companion.");
                return false;
            }

            IFarmProcess farm = baritone.getFarmProcess();
            if (farm == null) {
                SomAIChat.error(owner, "PlayerEngine: farm process not available.");
                return false;
            }

            if (centerOrNull != null) {
                farm.farm(range, centerOrNull);
            } else {
                farm.farm(range);
            }

            SomAIChat.action(owner, "Automaton: farming.");
            return true;
        } catch (Throwable t) {
            SomAIChat.error(owner, "PlayerEngine farm failed: " + t.getClass().getSimpleName());
            return false;
        }
    }

    private static IBaritone getBaritoneForEntity(LivingEntity entity) {
        try {
            return BaritoneAPI.getProvider().getBaritone(entity);
        } catch (Throwable t) {
            return null;
        }
    }

    public static AutomatonStatus getAutomatonStatus(LivingEntity automatoneEntity) {
        if (!isAvailable() || automatoneEntity == null) {
            return AutomatonStatus.unavailable();
        }

        try {
            IBaritone baritone = getBaritoneForEntity(automatoneEntity);
            if (baritone == null) {
                return AutomatonStatus.unavailable();
            }

            boolean pathing = false;
            String goal = "";

            IPathingBehavior pathingBehavior = baritone.getPathingBehavior();
            if (pathingBehavior != null) {
                pathing = invokeBoolean(pathingBehavior, "isPathing");
                Object goalObj = invokeObject(pathingBehavior, "getGoal");
                if (goalObj == null) {
                    goalObj = invokeObject(pathingBehavior, "goal");
                }
                if (goalObj != null) {
                    goal = safeToString(goalObj);
                }
            }

            return new AutomatonStatus(true, pathing, goal == null ? "" : goal);
        } catch (Throwable t) {
            return AutomatonStatus.unavailable();
        }
    }

    private static boolean invokeBoolean(Object target, String methodName) {
        try {
            var m = target.getClass().getMethod(methodName);
            Object v = m.invoke(target);
            return v instanceof Boolean b && b;
        } catch (Throwable t) {
            return false;
        }
    }

    private static Object invokeObject(Object target, String methodName) {
        try {
            var m = target.getClass().getMethod(methodName);
            return m.invoke(target);
        } catch (Throwable t) {
            return null;
        }
    }

    private static String safeToString(Object obj) {
        try {
            return String.valueOf(obj);
        } catch (Throwable t) {
            return "";
        }
    }
}
