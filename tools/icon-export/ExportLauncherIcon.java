import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import org.w3c.dom.Node;

/**
 * Membuat ikon 512x512 untuk daftar Play Store.
 *
 * Kenapa perlu file terpisah: ikon di dalam app berbentuk adaptive icon (vektor),
 * karena itu yang dipakai Android untuk memasang ikon di layar utama. Play Console
 * tidak membaca aset itu; Play Console meminta satu file PNG 512x512, bentuk persegi
 * penuh tanpa sudut membulat, karena masking dan shadow dipasang oleh Play sendiri.
 *
 * Geometrinya bukan gambar baru. Angka di bawah ini transkripsi dari
 *   app/src/main/res/drawable/ic_launcher_foreground.xml  (jejak kaki, InkSoft)
 *   app/src/main/res/drawable/ic_launcher_background.xml  (latar PeachAccent solid)
 * termasuk skala 2.8 dan geser 20.4 milik grup-nya.
 *
 * Menjalankannya: lihat tools/icon-export/export-icon.sh
 *
 * Dua pilihan ukuran jejak kaki (argumen kedua):
 *   "small" (bawaan) jejak kaki sama persis dengan ikon adaptive, jadi hasilnya di
 *           daftar Play identik dengan yang tampak di layar utama.
 *   "large"           jejak kaki diperbesar sampai batas yang masih muat di dalam
 *           area aman ikon adaptive (lihat [largestSafeScale]). Halaman spesifikasi
 *           ikon Google Play menyarankan memakai ruang aset sepenuhnya kalau
 *           bentuknya memungkinkan, dan logo kecil di tengah bidang besar memang
 *           terasa hilang saat ditampilkan di daftar.
 * Pusat jejak kakinya tetap sama di kedua pilihan, jadi yang berubah hanya ukurannya.
 */
public final class ExportLauncherIcon {

    private static final int SIZE = 512;

    /** Sama dengan viewportWidth/Height di kedua drawable. */
    private static final double VIEWPORT = 108.0;

    /** Grup di ic_launcher_foreground.xml: scaleX/scaleY 2.8, translateX/Y 20.4. */
    private static final double GROUP_SCALE = 2.8;
    private static final double GROUP_SHIFT = 20.4;

    /**
     * Sisa sedikit di dalam batas aman, supaya jejak kakinya tidak menempel persis
     * di garis area aman. 0.97 berarti 3% lebih kecil dari batas maksimalnya.
     */
    private static final double LARGE_MARGIN = 0.97;

    /** PeachAccent, warna latar ikon. */
    private static final Color BACKGROUND = new Color(0xFF, 0xB2, 0x7A);

    /** InkSoft, warna jejak kaki. */
    private static final Color FOREGROUND = new Color(0x3A, 0x2E, 0x26);

    /**
     * Lima bentuk jejak kaki: cx, cy, rx, ry dalam koordinat grup (sebelum skala).
     *
     * Di drawable-nya tiap bentuk ditulis sebagai dua busur yang bertemu di titik
     * paling kiri dan paling kanan elips, jadi hasilnya memang satu elips penuh.
     */
    private static final double[][] PAW = {
        {6.4, 9.8, 2.2, 2.4},
        {10.2, 6.6, 2.3, 2.6},
        {13.8, 6.6, 2.3, 2.6},
        {17.6, 9.8, 2.2, 2.4},
        {12.0, 16.0, 5.2, 4.4},
    };

    public static void main(String[] args) throws Exception {
        File target = new File(args.length > 0 ? args[0] : "store/ic_launcher_512.png");
        boolean larger = args.length > 1 && args[1].equals("large");
        double largeScale = largestSafeScale() * LARGE_MARGIN;
        double scale = larger ? largeScale : GROUP_SCALE;

        // Pusat jejak kaki di ikon adaptive, dihitung dari kotak yang benar-benar
        // memuat kelima bentuk. Pergeserannya dipilih supaya pusat ini tidak
        // berpindah saat ukurannya diubah.
        double[] bounds = pawBounds();
        double centerX = bounds[0] + bounds[2] / 2;
        double centerY = bounds[1] + bounds[3] / 2;
        double shiftX = centerX * GROUP_SCALE + GROUP_SHIFT - centerX * scale;
        double shiftY = centerY * GROUP_SCALE + GROUP_SHIFT - centerY * scale;

        // ARGB supaya hasilnya PNG 32-bit, tapi seluruh kanvas diisi rata, jadi
        // tidak ada satu pun piksel yang tembus pandang.
        BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        g.setColor(BACKGROUND);
        g.fillRect(0, 0, SIZE, SIZE);

        g.setColor(FOREGROUND);
        double unit = SIZE / VIEWPORT;
        for (double[] shape : PAW) {
            double cx = (shape[0] * scale + shiftX) * unit;
            double cy = (shape[1] * scale + shiftY) * unit;
            double rx = shape[2] * scale * unit;
            double ry = shape[3] * scale * unit;

            g.fill(new Ellipse2D.Double(cx - rx, cy - ry, rx * 2, ry * 2));
        }
        g.dispose();

        File parent = target.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        writeSrgbPng(image, target);

        // Dibaca ulang dari file, bukan dari gambar di memori, supaya yang diperiksa
        // benar-benar isi file PNG yang akan diunggah.
        BufferedImage written = ImageIO.read(target);
        if (written == null || written.getWidth() != SIZE || written.getHeight() != SIZE) {
            throw new IllegalStateException("file PNG-nya tidak terbaca atau ukurannya salah");
        }

        int[] box = foregroundBounds(written);
        double safeEdge = SIZE * (1 - 66.0 / VIEWPORT) / 2;

        System.out.println("file      : " + target.getAbsolutePath());
        System.out.println("ukuran    : " + SIZE + "x" + SIZE);
        System.out.println("byte      : " + target.length());
        System.out.println("skala     : " + String.format("%.3f", scale)
            + (larger
                ? "  (batas aman " + String.format("%.3f", largestSafeScale()) + ", diambil 97%-nya)"
                : "  (sama dengan ikon adaptive)"));
        System.out.println("sudut     : " + hex(written.getRGB(2, 2)));
        int padPixelY = (int) Math.round((16.0 * scale + shiftY) * SIZE / VIEWPORT);
        System.out.println("bantalan  : " + hex(written.getRGB(SIZE / 2, padPixelY)));
        System.out.println("transparan: " + countTransparent(written) + " piksel");
        System.out.println("jejak kaki: x " + box[0] + ".." + box[2] + ", y " + box[1] + ".." + box[3]);
        System.out.println("lebar     : " + String.format("%.1f", 100.0 * (box[2] - box[0] + 1) / SIZE) + "% dari sisi ikon");
        System.out.println("area aman : " + (int) safeEdge + ".." + (int) (SIZE - safeEdge)
            + (isInsideSafeZone(box, safeEdge) ? "  (jejak kaki seluruhnya di dalam)" : "  DI LUAR AREA AMAN"));
    }

    /**
     * Menulis PNG sekaligus menandainya sRGB secara eksplisit.
     *
     * Halaman spesifikasi ikon Google Play meminta warna sRGB, dan PNG tanpa
     * keterangan ruang warna memang dibaca sebagai sRGB, tapi menandainya langsung
     * membuat file-nya tidak bergantung pada kebiasaan pembaca. Tanpa potongan sRGB
     * ini, java.awt hanya menghasilkan IHDR, IDAT, dan IEND.
     */
    private static void writeSrgbPng(BufferedImage image, File target) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("png").next();
        ImageWriteParam params = writer.getDefaultWriteParam();
        IIOMetadata metadata = writer.getDefaultImageMetadata(new ImageTypeSpecifier(image), params);

        String format = metadata.getNativeMetadataFormatName();
        IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(format);
        // Buang keterangan ruang warna bawaan (kalau ada) supaya tidak ada dua
        // keterangan sekaligus, lalu pasang satu saja.
        for (String tag : new String[] {"sRGB", "gAMA", "cHRM", "iCCP"}) {
            for (Node node : nodesNamed(root, tag)) {
                root.removeChild(node);
            }
        }
        IIOMetadataNode srgb = new IIOMetadataNode("sRGB");
        // Java memakai nama, bukan angka: "Perceptual" sama dengan nilai 0 di PNG.
        srgb.setAttribute("renderingIntent", "Perceptual");
        root.appendChild(srgb);
        metadata.mergeTree(format, root);

        try (ImageOutputStream output = ImageIO.createImageOutputStream(target)) {
            writer.setOutput(output);
            writer.write(null, new IIOImage(image, null, metadata), params);
        } finally {
            writer.dispose();
        }
    }

    private static List<Node> nodesNamed(IIOMetadataNode parent, String tag) {
        List<Node> found = new ArrayList<>();
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node.getNodeName().equals(tag)) {
                found.add(node);
            }
        }
        return found;
    }

    /**
     * Skala terbesar yang masih menaruh seluruh jejak kaki di dalam area aman
     * ikon adaptive (66 dari 108, jadi 33 di kiri dan kanan titik tengah).
     *
     * Dihitung, bukan dipatok angka: kalau bentuk jejak kakinya diubah, batasnya
     * ikut berubah sendiri. Pergeseran yang dipakai menjaga pusatnya tetap, jadi
     * syaratnya cukup setengah tingginya tidak melewati batas tersebut.
     */
    private static double largestSafeScale() {
        double[] bounds = pawBounds();
        double centerX = bounds[0] + bounds[2] / 2;
        double centerY = bounds[1] + bounds[3] / 2;
        double anchorX = centerX * GROUP_SCALE + GROUP_SHIFT;
        double anchorY = centerY * GROUP_SCALE + GROUP_SHIFT;
        double safeNear = (VIEWPORT - 66.0) / 2;
        double safeFar = VIEWPORT - safeNear;

        double byWidth = Math.min(anchorX - safeNear, safeFar - anchorX) * 2 / bounds[2];
        double byHeight = Math.min(anchorY - safeNear, safeFar - anchorY) * 2 / bounds[3];
        return Math.min(byWidth, byHeight);
    }

    /** Kotak terkecil yang memuat kelima bentuk, dalam koordinat grup. */
    private static double[] pawBounds() {
        double left = Double.MAX_VALUE;
        double top = Double.MAX_VALUE;
        double right = -Double.MAX_VALUE;
        double bottom = -Double.MAX_VALUE;
        for (double[] shape : PAW) {
            left = Math.min(left, shape[0] - shape[2]);
            top = Math.min(top, shape[1] - shape[3]);
            right = Math.max(right, shape[0] + shape[2]);
            bottom = Math.max(bottom, shape[1] + shape[3]);
        }
        return new double[] {left, top, right - left, bottom - top};
    }

    /** Kotak terkecil yang memuat seluruh piksel jejak kaki: x0, y0, x1, y1. */
    private static int[] foregroundBounds(BufferedImage image) {
        int x0 = SIZE;
        int y0 = SIZE;
        int x1 = -1;
        int y1 = -1;
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                if ((image.getRGB(x, y) & 0xFFFFFF) == (FOREGROUND.getRGB() & 0xFFFFFF)) {
                    x0 = Math.min(x0, x);
                    y0 = Math.min(y0, y);
                    x1 = Math.max(x1, x);
                    y1 = Math.max(y1, y);
                }
            }
        }
        return new int[] {x0, y0, x1, y1};
    }

    /**
     * Ikon adaptive hanya menjamin area aman 66 dari 108 yang tidak dipotong oleh
     * masking launcher. Karena Play juga memasang masking sendiri, jejak kakinya
     * harus tetap berada di dalam area itu.
     */
    private static boolean isInsideSafeZone(int[] box, double safeEdge) {
        return box[0] >= safeEdge && box[1] >= safeEdge && box[2] <= SIZE - safeEdge && box[3] <= SIZE - safeEdge;
    }

    private static String hex(int argb) {
        return String.format("#%06X", argb & 0xFFFFFF);
    }

    /** Harus 0: Play menolak ikon yang punya area tembus pandang. */
    private static int countTransparent(BufferedImage image) {
        int count = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) >>> 24) != 0xFF) {
                    count++;
                }
            }
        }
        return count;
    }
}
