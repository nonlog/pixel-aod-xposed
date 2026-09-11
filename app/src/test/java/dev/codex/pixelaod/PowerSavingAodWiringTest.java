package dev.codex.pixelaod;
import org.junit.Test;
import java.nio.charset.StandardCharsets; import java.nio.file.*; import static org.junit.Assert.*;
public class PowerSavingAodWiringTest {
 private static String source(String n)throws Exception{Path r=Paths.get(System.getProperty("user.dir")).toAbsolutePath();while(r!=null&&!Files.exists(r.resolve("settings.gradle")))r=r.getParent();assertNotNull(r);return new String(Files.readAllBytes(r.resolve("app/src/main/java/dev/codex/pixelaod/"+n+".java")),StandardCharsets.UTF_8);}
 @Test public void chargingUsesVendorLifecycle()throws Exception{String t=source("PowerSavingAodController");assertTrue(t.contains("callMethod(o,"showClock",0)"));assertTrue(t.contains("callMethod(o,"setHideAlarm")"));assertFalse(t.contains("PowerManager.wakeUp"));assertFalse(t.contains("AlarmManager"));assertFalse(t.contains("postDelayed"));}
 @Test public void notificationUsesNativePeekLifetime()throws Exception{String t=source("PixelPeekNotificationController");assertTrue(t.contains("startPowerSavingNotificationAod"));assertTrue(t.contains("endPowerSavingNotificationAod"));assertTrue(t.contains("onDetachedFromWindow"));}
 @Test public void nativeModeIsReadNotRewritten()throws Exception{String t=source("PowerSavingAodController");assertFalse(t.contains("Settings.Secure.put"));assertTrue(source("PowerSavingAodPolicy").contains("energy-saving"));}
}
