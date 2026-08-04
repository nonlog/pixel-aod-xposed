package dev.codex.pixelaod;

final class CouiMediaLine {
    final String title;
    final String artist;

    private CouiMediaLine(String title, String artist) {
        this.title = title;
        this.artist = artist;
    }

    static CouiMediaLine of(CharSequence title, CharSequence artist) {
        String resolvedTitle = title == null ? "" : title.toString();
        String resolvedArtist = artist == null ? "" : artist.toString();
        if (resolvedTitle.equals(resolvedArtist)) {
            resolvedArtist = "";
        }
        return new CouiMediaLine(resolvedTitle, resolvedArtist);
    }

    boolean isEmpty() {
        return title.isEmpty();
    }

    String signature() {
        return artist.isEmpty() ? title : title + " - " + artist;
    }
}
