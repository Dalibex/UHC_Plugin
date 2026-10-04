package me.dalibex.UHC_DBasic.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;

import org.bukkit.Material;

/** Pure selection and timing policy for item roulettes. */
final class ItemRouletteAnimation {

    private ItemRouletteAnimation() {
    }

    static Material select(List<Material> candidates, RandomGenerator random) {
        Objects.requireNonNull(random, "random");
        if (candidates.isEmpty()) throw new IllegalArgumentException("Candidates cannot be empty");
        return candidates.get(random.nextInt(candidates.size()));
    }

    static List<Material> frames(List<Material> candidates, Material result, int steps,
                                 RandomGenerator random) {
        if (steps < 1) throw new IllegalArgumentException("Steps must be positive");
        Objects.requireNonNull(result, "result");
        List<Material> frames = new ArrayList<>(steps);
        Material previous = null;

        for (int i = 0; i < steps - 1; i++) {
            Material next = select(candidates, random);
            if (candidates.size() > 1 && next == previous) {
                int current = candidates.indexOf(next);
                next = candidates.get((current + 1 + random.nextInt(candidates.size() - 1)) % candidates.size());
            }
            frames.add(next);
            previous = next;
        }
        if (candidates.size() > 1 && previous == result) {
            int current = candidates.indexOf(result);
            frames.set(frames.size() - 1, candidates.get((current + 1) % candidates.size()));
        }
        frames.add(result);
        return List.copyOf(frames);
    }

    static long delayAfterFrame(int frameIndex, int steps, long initialDelay, long finalDelay) {
        if (steps <= 1) return finalDelay;
        double progress = (double) frameIndex / (steps - 1);
        double eased = progress * progress;
        return Math.round(initialDelay + ((finalDelay - initialDelay) * eased));
    }

    static long totalDurationTicks(int steps, long initialDelay, long finalDelay, long resultHold) {
        long total = resultHold;
        for (int i = 0; i < steps - 1; i++) {
            total += delayAfterFrame(i, steps, initialDelay, finalDelay);
        }
        return total;
    }
}
