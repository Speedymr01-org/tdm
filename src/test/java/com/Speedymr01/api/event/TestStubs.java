package com.Speedymr01.api.event;

import org.bukkit.entity.Player;

import java.lang.reflect.Proxy;
import java.util.UUID;

/**
 * Minimal test stubs for Bukkit interfaces used in API event tests.
 */
public final class TestStubs {

    private TestStubs() {}

    public static Player player(String name, UUID uuid) {
        return (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getName":     return name;
                        case "getUniqueId": return uuid;
                        case "equals":      return proxy == args[0];
                        case "hashCode":    return System.identityHashCode(proxy);
                        case "toString":    return "Player{" + name + "}";
                        default:
                            Class<?> ret = method.getReturnType();
                            if (!ret.isPrimitive()) return null;
                            if (ret == boolean.class) return false;
                            if (ret == int.class)     return 0;
                            if (ret == long.class)    return 0L;
                            if (ret == double.class)  return 0.0;
                            if (ret == float.class)   return 0.0f;
                            return null;
                    }
                }
        );
    }

    public static Player player(String name) {
        return player(name, UUID.randomUUID());
    }
}
