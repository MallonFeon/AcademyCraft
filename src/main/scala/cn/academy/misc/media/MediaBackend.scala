package cn.academy.misc.media

import cn.academy.core.AcademyCraft
import cn.academy.misc.media.MediaBackend.PlayInfo
import cn.lambdalib.annoreg.core.Registrant
import cn.lambdalib.util.datapart.{DataPart, EntityData, RegDataPart}
import cn.lambdalib.util.generic.RegistryUtils
import cn.lambdalib.util.helper.TickScheduler
import com.google.common.base.Preconditions
import cpw.mods.fml.relauncher.{Side, SideOnly}
import net.minecraft.client.Minecraft
import net.minecraft.client.audio.{ISound, MusicTicker, SoundHandler, SoundManager}
import net.minecraft.entity.player.EntityPlayer
import net.minecraftforge.common.config.Property
import paulscode.sound.{Library, SoundSystem, SoundSystemConfig}


object MediaBackend {

  def getDisplayTime(secs: Float): String = {
    def wrapTime(x: Int) = if (x < 10) s"0$x" else x.toString

    val totalSecs = secs.toInt
    val minutes = totalSecs / 60
    val seconds = totalSecs % 60
    wrapTime(minutes) + ":" + wrapTime(seconds)
  }

  case class PlayInfo(media: Media, paused: Boolean, time: Float) {

    @inline
    def displayTime: String = getDisplayTime(time)

  }

  def apply(player: EntityPlayer): MediaBackend = EntityData.get(player).getPart(classOf[MediaBackend])

  def apply(): MediaBackend = apply(Minecraft.getMinecraft.thePlayer)

}

private object MediaBackendHelper {

  private var init = false
  private var volumeProperty_ : Property = null
  private var sndLibrary_ : Library = null
  private var sndSystem_ : SoundSystem = null

  def checkInit() = {
    if (!init) {
      volumeProperty_ = AcademyCraft.config.get("media_player", "volume", 1.0, "Media Player's volume")

      val fSndManager = RegistryUtils.getObfField(classOf[SoundHandler], "sndManager", "field_147694_f")
      val sndMgr = fSndManager.get(Minecraft.getMinecraft.getSoundHandler)

      val fSndSystem = RegistryUtils.getObfField(classOf[SoundManager], "sndSystem", "field_148620_e")
      fSndSystem.setAccessible(true)
      sndSystem_ = fSndSystem.get(sndMgr).asInstanceOf[SoundSystem]

      val fSndLibrary = classOf[SoundSystem].getDeclaredField("soundLibrary")
      fSndLibrary.setAccessible(true)
      sndLibrary_ = fSndLibrary.get(sndSystem_).asInstanceOf[Library]

      init = true
    }
  }

  def volumeProperty = { checkInit(); volumeProperty_ }
  def sndLibrary = { checkInit(); sndLibrary_ }
  def sndSystem = { checkInit(); sndSystem_ }

  def stopVanillaSound() = {
    val musicTicker: MusicTicker = RegistryUtils.getFieldInstance(classOf[Minecraft],
      Minecraft.getMinecraft, "mcMusicTicker", "field_147126_aw")
    val playing: ISound = RegistryUtils.getFieldInstance(classOf[MusicTicker], musicTicker, "field_147678_c")
    if (playing != null) {
      Minecraft.getMinecraft.getSoundHandler.stopSound(playing)
    }
  }

}

@Registrant
@SideOnly(Side.CLIENT)
@RegDataPart(value=classOf[EntityPlayer], side=Array(Side.CLIENT))
class MediaBackend extends DataPart[EntityPlayer] {
  import MediaBackendHelper._

  final val MEDIA_ID = "AC_MediaPlayer"

  private var playState: Option[PlayState] = None
  private var lastPlay_ : Option[Media] = None


  setTick(true)

  val scheduler = new TickScheduler

  scheduler.every(5).run(() => {
    currentPlaying match {
      case Some(PlayInfo(_, false, _)) => stopVanillaSound()
      case _ =>
    }
  })

  scheduler.every(5).run(() => {
    // Synchronize play state
    playState match {
      case Some(state) =>
        if (!sndSystem.playing(MEDIA_ID)) {
          playState = None
        }
      case _ =>
    }
  })

  def play(media: Media): Unit = {
    sndSystem.removeSource(MEDIA_ID)

    sndSystem.newStreamingSource(true, MEDIA_ID,
      media.source, MEDIA_ID + ".ogg", false,
      0, 0, 0,
      SoundSystemConfig.ATTENUATION_NONE,  SoundSystemConfig.getDefaultAttenuation)

    sndSystem.setVolume(MEDIA_ID, volumeProperty.getDouble.toFloat)
    sndSystem.play(MEDIA_ID)

    playState = Some(PlayState(media))
    lastPlay_ = Some(media)
  }

  def pauseCurrent(): Unit = playState match {
    case Some(_) => sndSystem.pause(MEDIA_ID)
    case _ =>
  }

  def continueCurrent(): Unit = playState match {
    case Some(_) => sndSystem.play(MEDIA_ID)
    case _ =>
  }

  def stopCurrent(): Unit = playState match {
    case Some(_) =>
      sndSystem.stop(MEDIA_ID)
      playState = None
    case _ =>
  }

  def currentPlaying: Option[PlayInfo] = playState.flatMap { case PlayState(media) => {
    Option(sndLibrary.getSource(MEDIA_ID)) match {
      case Some(src) =>
        Some(PlayInfo(media, src.paused(), src.millisecondsPlayed() / 1000.0f))
      case _ => None
    }
  }}

  def lastPlayed: Option[Media] = lastPlay_

  def getVolume: Float = volumeProperty.getDouble.toFloat

  def setVolume(value: Float) = {
    volumeProperty.set(value.toDouble)
    sndSystem.setVolume(MEDIA_ID, value)
  }

  override def tick(): Unit = {
    scheduler.runTick()
  }

  case class PlayState(media: Media)
}
