package cn.academy.core;

import org.apache.logging.log4j.Logger;

import cn.academy.api.ctrl.ControlMessage;
import cn.academy.core.proxy.ProxyCommon;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.common.SidedProxy;


/**
 * Academy Craft Mod 主类
 * @author acaly
 *
 */

@Mod(modid = "academy-craft", name = "AcademyCraft", version = AcademyCraftMod.VERSION)
public class AcademyCraftMod {

	/**
	 * 当前版本
	 */
	public static final String VERSION = "0.0.1dev";

	public static final String NET_CHANNEL = "academy-network";

	/**
	 * 主类实例
	 */
	@Instance("academy-craft")
	public static AcademyCraftMod INSTANCE;
	

	@SidedProxy(clientSide = "cn.academy.core.proxy.ProxyClient", serverSide = "cn.academy.core.proxy.ProxyCommon")
	/**
	 * 加载代理
	 */
	public static ProxyCommon proxy;

	/**
	 * 日志
	 */
	public static Logger log = FMLLog.getLogger();
	
	/**
	 * 网络发包处理实例
	 */
	public static SimpleNetworkWrapper netHandler = NetworkRegistry.INSTANCE.newSimpleChannel(AcademyCraftMod.NET_CHANNEL);
	
	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		proxy.preInit();
	}

	@EventHandler
	public void init(FMLInitializationEvent event) {
		proxy.init();
	}

	@EventHandler
	public void postInit(FMLPostInitializationEvent event) {
		proxy.postInit();
	}
	
	private static int nextNetID = 0;
	/**
	 * 获取下一个空闲的网络channelID。
	 */
	public static int getNextChannelID() {
		return nextNetID++;
	}
}
