package com.demonicrous.motionui.font;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import org.junit.Test;

public final class UnicodeFontResourcesTest {
    private static final String CYRILLIC_PAGE =
            "/assets/minecraft/textures/font/unicode_page_04.png";

    @Test
    public void cyrillicHdAtlasHasOnlyHardAlpha() throws IOException {
        InputStream input = UnicodeFontResourcesTest.class.getResourceAsStream(CYRILLIC_PAGE);
        assertNotNull("Missing Cyrillic Unicode atlas", input);

        BufferedImage image;
        try {
            image = ImageIO.read(input);
        } finally {
            input.close();
        }

        assertNotNull("Unreadable Cyrillic Unicode atlas", image);
        assertEquals("HD atlas width changed", 512, image.getWidth());
        assertEquals("HD atlas height changed", 512, image.getHeight());

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int alpha = image.getColorModel().getAlpha(image.getRaster().getDataElements(x, y, null));
                assertTrue("Partial alpha at " + x + "," + y + ": " + alpha,
                        alpha == 0 || alpha == 255);
            }
        }
    }
}
