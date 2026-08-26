package dev.codex.pixelaod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class NativeSystemUiMediaAdapterTest {
    @Test
    public void activeNativeMediaNormalizesSemanticFields() {
        FakeMediaData data = new FakeMediaData(true, 10, "com.example.player",
                "Song", "Artist", "Player");

        NativeSystemUiMediaAdapter.Snapshot snapshot =
                NativeSystemUiMediaAdapter.snapshotFromMediaDataForTest(data);

        assertTrue(snapshot.authoritative);
        assertTrue(snapshot.present);
        assertTrue(snapshot.active);
        assertEquals(10, snapshot.userId);
        assertEquals("com.example.player", snapshot.packageName);
        assertEquals("Song", snapshot.title);
        assertEquals("Artist", snapshot.artist);
    }

    @Test
    public void inactiveNativeMediaIsAuthoritativeEmpty() {
        FakeMediaData data = new FakeMediaData(false, 0, "com.example.player",
                "Old Song", "Old Artist", "Player");

        NativeSystemUiMediaAdapter.Snapshot snapshot =
                NativeSystemUiMediaAdapter.snapshotFromMediaDataForTest(data);

        assertTrue(snapshot.authoritative);
        assertFalse(snapshot.present);
        assertFalse(snapshot.active);
        assertEquals("com.example.player", snapshot.packageName);
        assertEquals("", snapshot.title);
        assertEquals("", snapshot.artist);
    }

    @Test
    public void nullNativeMediaIsAuthoritativeEmpty() {
        NativeSystemUiMediaAdapter.Snapshot snapshot =
                NativeSystemUiMediaAdapter.snapshotFromMediaDataForTest(null);

        assertTrue(snapshot.authoritative);
        assertFalse(snapshot.present);
        assertFalse(snapshot.active);
    }

    public static final class FakeMediaData {
        private final boolean active;
        private final int userId;
        private final String packageName;
        private final String song;
        private final String artist;
        private final String app;

        FakeMediaData(boolean active, int userId, String packageName, String song,
                String artist, String app) {
            this.active = active;
            this.userId = userId;
            this.packageName = packageName;
            this.song = song;
            this.artist = artist;
            this.app = app;
        }

        public boolean getActive() {
            return active;
        }

        public int getUserId() {
            return userId;
        }

        public String getPackageName() {
            return packageName;
        }

        public String getSong() {
            return song;
        }

        public String getArtist() {
            return artist;
        }

        public String getApp() {
            return app;
        }

        public Object getAppIcon() {
            return null;
        }
    }
}
