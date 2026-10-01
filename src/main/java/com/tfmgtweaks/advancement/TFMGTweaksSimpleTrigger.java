package com.tfmgtweaks.advancement;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.tfmgtweaks.TFMGTweaks;

import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;

public class TFMGTweaksSimpleTrigger implements CriterionTrigger<TFMGTweaksSimpleTrigger.Instance> {

    private final ResourceLocation id;
    private final Map<PlayerAdvancements, Set<Listener<Instance>>> listeners = Maps.newHashMap();

    public TFMGTweaksSimpleTrigger(String id) {
        this.id = ResourceLocation.fromNamespaceAndPath(TFMGTweaks.MOD_ID, id);
    }

    public ResourceLocation getId() {
        return id;
    }

    public void trigger(ServerPlayer player) {
        PlayerAdvancements playerAdvancements = player.getAdvancements();
        Set<Listener<Instance>> playerListeners = listeners.get(playerAdvancements);
        if (playerListeners == null) {
            return;
        }
        List<Listener<Instance>> toRun = new LinkedList<>(playerListeners);
        toRun.forEach(listener -> listener.run(playerAdvancements));
    }

    @Override
    public void addPlayerListener(PlayerAdvancements playerAdvancements, Listener<Instance> listener) {
        listeners.computeIfAbsent(playerAdvancements, k -> new HashSet<>()).add(listener);
    }

    @Override
    public void removePlayerListener(PlayerAdvancements playerAdvancements, Listener<Instance> listener) {
        Set<Listener<Instance>> playerListeners = listeners.get(playerAdvancements);
        if (playerListeners == null) {
            return;
        }
        playerListeners.remove(listener);
        if (playerListeners.isEmpty()) {
            listeners.remove(playerAdvancements);
        }
    }

    @Override
    public void removePlayerListeners(PlayerAdvancements playerAdvancements) {
        listeners.remove(playerAdvancements);
    }

    public Instance instance() {
        return new Instance();
    }

    @Override
    public Codec<Instance> codec() {
        return Instance.CODEC;
    }

    public static class Instance implements SimpleCriterionTrigger.SimpleInstance {

        private static final Codec<Instance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player)
        ).apply(instance, Instance::new));

        private final Optional<ContextAwarePredicate> player;

        public Instance() {
            player = Optional.empty();
        }

        public Instance(Optional<ContextAwarePredicate> player) {
            this.player = player;
        }

        @Override
        public Optional<ContextAwarePredicate> player() {
            return player;
        }
    }
}
