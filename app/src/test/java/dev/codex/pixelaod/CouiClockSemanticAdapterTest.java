package dev.codex.pixelaod;

import android.graphics.drawable.Drawable;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

/** Locks down the complete semantic input handed to the COUI host before its five-icon cap. */
public final class CouiClockSemanticAdapterTest {
    @Test
    public void snapshotRetainsAllFilteredIconsForHostOverflowCalculation() {
        List<Drawable> icons = Arrays.asList(
                null, null, null, null, null, null, null, null);

        CouiClockSemanticAdapter.Snapshot snapshot = new CouiClockSemanticAdapter.Snapshot(
                icons,
                CouiClockSemanticAdapter.MediaData.empty(),
                CouiClockPresentationModel.AodContent.notifications(icons.size()));

        assertEquals(8, snapshot.notificationIcons.size());
        assertEquals(8, snapshot.content.notificationIconCount());
    }
}
