package melonslise.locks.client;

import melonslise.locks.Locks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * Client-only entry point for the Mods menu config button.
 *
 * <p>The common {@link Locks} constructor only registers the three config specs; NeoForge needs an
 * {@link IConfigScreenFactory} extension point before it shows a "Config" button. Keeping that registration
 * in a {@code Dist.CLIENT} mod class means the client GUI classes are never loaded on a dedicated server.
 */
@Mod(value = Locks.ID, dist = Dist.CLIENT)
public final class LocksClientMod
{
	public LocksClientMod(ModContainer container)
	{
		container.registerExtensionPoint(IConfigScreenFactory.class,
			(selectedContainer, parent) -> new ConfigurationScreen(container, parent));
	}
}
