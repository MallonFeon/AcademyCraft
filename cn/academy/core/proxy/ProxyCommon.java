package cn.academy.core.proxy;

import cn.academy.api.ctrl.EventHandlerServer;
import cn.academy.core.AcademyCraftMod;
import net.minecraft.command.CommandHandler;

public class ProxyCommon {
	
	public void preInit() {}
	
	public void init() {
		AcademyCraftMod.INSTANCE.log.info("Loading common proxy of Academy Craft.");
		EventHandlerServer.init();
	}
	
	public void postInit() {}
	
	public void commandInit(CommandHandler cm) {}
	
}
