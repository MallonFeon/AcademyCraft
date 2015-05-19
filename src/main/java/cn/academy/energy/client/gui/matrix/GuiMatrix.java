/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * This project is open-source, and it is distributed under  
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.energy.client.gui.matrix;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cn.academy.energy.block.ContainerMatrix;
import cn.academy.energy.block.TileMatrix;
import cn.academy.energy.client.gui.matrix.GuiMatrixSync.ActionResult;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegSubmoduleInit;
import cn.liutils.cgui.gui.LIGui;
import cn.liutils.cgui.gui.LIGuiContainer;
import cn.liutils.cgui.gui.Widget;
import cn.liutils.cgui.gui.annotations.GuiCallback;
import cn.liutils.cgui.gui.component.DrawTexture;
import cn.liutils.cgui.gui.component.TextBox;
import cn.liutils.cgui.gui.event.FrameEvent;
import cn.liutils.cgui.gui.event.FrameEvent.FrameEventHandler;
import cn.liutils.cgui.gui.event.MouseDownEvent;
import cn.liutils.cgui.loader.EventLoader;
import cn.liutils.cgui.loader.xml.CGUIDocLoader;
import cn.liutils.cgui.utils.Color;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * TODO: Localization
 * @author WeAthFolD
 */
@Registrant
@SideOnly(Side.CLIENT)
@RegSubmoduleInit
public class GuiMatrix extends LIGuiContainer {
	
	static LIGui loaded;
	
	public static void init() {
		loaded = CGUIDocLoader.load(new ResourceLocation("academy:guis/matrix.xml"));
	}
	
	//--------
	
	//Callbacks
	CheckCallback checkCallback;
	
	//Synced states
	boolean receivedSync;
	
	boolean isLoaded;
	
	String ssid;
	int nodes;
	int capacity;
	int latency;
	int range;
	
	//Action
	boolean waitingForResult;
	long resultReceivedTime; //Used for anim display time ctrl.
	ActionResult result;
	
	//Meta objects
	final TileMatrix tile;
	
	final ContainerMatrix container;
	
	final EntityPlayer player;
	
	Widget pageMain, pageSSID, pageCheck;

	public GuiMatrix(ContainerMatrix c) {
		super(c);
		tile = c.tile;
		container = c;
		player = Minecraft.getMinecraft().thePlayer;
		
		load();
	}
	
	public void receiveSync(NBTTagCompound tag) {
		if(!receivedSync) {
			receivedSync = true;
			
			isLoaded = tag.getBoolean("loaded");
			capacity = tag.getInteger("capacity");
			latency = tag.getInteger("latency");
			range = tag.getInteger("range");
			
			//Setup the info about matrix itself
			TextBox box;
			box = TextBox.get(pageMain.getWidget("text_cap2"));
			box.content = nodes + "/" + capacity;
			
			box = TextBox.get(pageMain.getWidget("text_latency2"));
			box.content = String.format("%d", latency);
			
			box = TextBox.get(pageMain.getWidget("text_range2"));
			box.content = String.format("%d", range);
			
			box = TextBox.get(pageMain.getWidget("text_ssid2"));
			if(isLoaded) {
				ssid = tag.getString("ssid");
				box.content = ssid;
			} else {
				box.content = "Not Loaded";
			}
		}
	}
	
	private void startWaiting() {
		waitingForResult = true;
		result = ActionResult.WAITING;
		
		checkCallback.updateCheckState();
		pageCheck.transform.doesDraw = true;
		pageMain.transform.doesListenKey = false;
	}
	
	/**
	 * May called by sync method or gui itself, to update state animation.
	 */
	public void receiveActionResult(ActionResult result, boolean needSync) {
		if(waitingForResult) {
			waitingForResult = false;
			this.result = result;
			resultReceivedTime = Minecraft.getSystemTime();
			checkCallback.updateCheckState();
			
			if(needSync) {
				receivedSync = false;
				GuiMatrixSync.sendSyncRequest(this);
			}
		}
	}
	
	private void load() {
		GuiMatrixSync.sendSyncRequest(this);
		
		LIGui gui = getGui();
		pageMain = loaded.getWidget("window_main").copy();
		pageSSID = loaded.getWidget("window_init").copy();
		pageCheck = loaded.getWidget("window_check").copy();
		
		gui.addWidget(pageMain);
		gui.addWidget(pageSSID);
		gui.addWidget(pageCheck);
		
		pageSSID.transform.doesDraw = false;
		pageCheck.transform.doesDraw = false;
		
		wrapButton(pageMain.getWidget("button_init"));
		
		wrapButton(pageSSID.getWidget("button_yes"));
		wrapButton(pageSSID.getWidget("button_no"));
		
		wrapButton(pageCheck.getWidget("button_close"));
		
		EventLoader.load(pageMain, new MainCallback());
		EventLoader.load(pageSSID, new SSIDCallback());
		EventLoader.load(pageCheck, checkCallback = new CheckCallback());
	}
	
	@Override
    public boolean isSlotActive() {
    	return pageMain.transform.doesListenKey;
    }
	
    protected boolean containerAcceptsKey(int key) {
    	return false;
    }
	
	private void wrapButton(Widget w) {
		DrawTexture drawer = w.getComponent("DrawTexture");
		final Color hoverColor = new Color(1, 1, 1, 1), idleColor = new Color(1, 1, 1, 0.3);
		drawer.color = idleColor;
		
		w.regEventHandler(new FrameEventHandler() {
			@Override
			public void handleEvent(Widget w, FrameEvent event) {
				DrawTexture drawer = w.getComponent("DrawTexture");
				drawer.color = event.hovering ? hoverColor : idleColor;
			}
		});
	}
	
	private void openInitWindow() {
		TextBox box = pageSSID.getWidget("text_1").getComponent("TextBox");
		DrawTexture 
			drawSSID = DrawTexture.get(pageSSID.getWidget("input_ssid")),
			drawOldPW = DrawTexture.get(pageSSID.getWidget("input_oldpw"));
		box.content = "";
		box.allowEdit = true;
		
		if(isLoaded) {
			drawSSID.enabled = false;
			drawOldPW.enabled = true;
		} else {
			drawSSID.enabled = true;
			drawOldPW.enabled = false;
		}
		
		TextBox.get(pageSSID.getWidget("text_2")).content = "";
		TextBox.get(pageSSID.getWidget("text_3")).content = "";
		
		pageMain.transform.doesListenKey = false;
		pageSSID.transform.doesDraw = true;
	}
	
	public class MainCallback {
		
		@GuiCallback("button_init")
		public void openDialogue(Widget w, MouseDownEvent event) {
			if(receivedSync)
				openInitWindow();
		}
		
		@GuiCallback
		public void blackout(Widget w, FrameEvent event) {
			if(!pageMain.transform.doesListenKey) {
				LIGui.drawBlackout();
			}
		}
		
	}

	public class SSIDCallback {
		
		@GuiCallback("button_yes")
		public void yesDown(Widget w, MouseDownEvent event) {
			startWaiting();
			
			if(!isLoaded) {
				//Do init
				String ssid = getContent(1), pw1 = getContent(2), pw2 = getContent(3);
				if(pw1.equals(pw2) && !ssid.isEmpty()) {
					GuiMatrixSync.fullInit(player, tile, ssid, pw1);
				} else {
					receiveActionResult(ActionResult.INVALID_INPUT, false);
				}
			} else {
				//Update pass
				String oldpw = getContent(1), pw1 = getContent(2), pw2 = getContent(3);
				if(pw1.equals(pw2)) {
					GuiMatrixSync.passwordUpdate(player, tile, oldpw, pw1);
				} else {
					receiveActionResult(ActionResult.INVALID_INPUT, false);
				}
			}
			
			pageSSID.transform.doesDraw = false;
		}
		
		@GuiCallback("button_no")
		public void noDown(Widget w, MouseDownEvent event) {
			//Close without doing anything
			pageSSID.transform.doesDraw = false;
			pageMain.transform.doesListenKey = true;
		}
		
		private String getContent(int iid) {
			return TextBox.get(pageSSID.getWidget("text_" + iid)).content;
		}
		
	}
	
	public class CheckCallback {
		Widget markDrawer, info, markBorder;
		
		public CheckCallback() {
			markBorder = pageCheck.getWidget("mark_check1");
			markDrawer = pageCheck.getWidget("mark_check2");
			info = pageCheck.getWidget("text_info");
		}
		
		public void updateCheckState() {
			DrawTexture.get(markDrawer).texture = result.markSrc;
			TextBox.get(info).content = result.getDescription();
		}
		
		@GuiCallback("mark_check1")
		public void updateAlpha(Widget w, FrameEvent event) {
			double alpha = 0.7 * 0.5 * (1 + Math.sin(Minecraft.getSystemTime() / 600.0)) + 0.3;
			DrawTexture.get(markBorder).color.a = alpha;
		}
		
		@GuiCallback("button_close")
		public void close(Widget w, MouseDownEvent event) {
			pageCheck.transform.doesDraw = false;
			pageMain.transform.doesListenKey = true;
		}
		
	}
	
}
