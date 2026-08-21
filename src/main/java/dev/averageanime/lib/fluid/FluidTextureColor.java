package dev.averageanime.lib.fluid;

import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.InputStream;

import javax.imageio.ImageIO;

/**
 * Average colour of a fluid's still texture, for tinting and fog.
 *
 * <p>Weighted by alpha, so the transparent margin of a texture does not drag the result toward black.
 * A fully transparent or unreadable texture falls back to white, which tints to no change rather than
 * to something obviously wrong.
 *
 * <p>This removes the need to maintain colour constants by hand alongside the textures they describe --
 * two copies of the same fact that drift the moment a texture is redrawn.
 */
public final class FluidTextureColor {

    private static final Logger LOGGER = LoggerFactory.getLogger("aa-modlib/fluid-colour");
    private static final Vector3f WHITE = new Vector3f(1.0f, 1.0f, 1.0f);

    private FluidTextureColor() {}

    /**
     * Reads {@code /assets/<modId>/textures/fluid/<fluidName>_still.png} off the classpath.
     *
     * @return the alpha-weighted mean colour, or white if the texture is missing, unreadable or blank
     */
    public static Vector3f of(String modId, String fluidName) {
        String path = "/assets/" + modId + "/textures/fluid/" + fluidName + "_still.png";
        try (InputStream stream = FluidTextureColor.class.getResourceAsStream(path)) {
            if (stream == null) {
                LOGGER.warn("No texture for fluid {} at {}", fluidName, path);
                return new Vector3f(WHITE);
            }
            BufferedImage image = ImageIO.read(stream);
            if (image == null) {
                LOGGER.warn("Could not read texture for fluid {}", fluidName);
                return new Vector3f(WHITE);
            }
            return average(image, fluidName);
        } catch (Exception e) {
            LOGGER.error("Error extracting colour for fluid {}", fluidName, e);
            return new Vector3f(WHITE);
        }
    }

    private static Vector3f average(BufferedImage image, String fluidName) {
        long totalR = 0, totalG = 0, totalB = 0, totalAlpha = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = image.getRGB(x, y);
                int alpha = (pixel >> 24) & 0xff;
                totalR += ((pixel >> 16) & 0xff) * (long) alpha;
                totalG += ((pixel >> 8) & 0xff) * (long) alpha;
                totalB += (pixel & 0xff) * (long) alpha;
                totalAlpha += alpha;
            }
        }
        if (totalAlpha == 0) {
            LOGGER.warn("Texture for fluid {} is fully transparent", fluidName);
            return new Vector3f(WHITE);
        }
        return new Vector3f(
                (float) (totalR / totalAlpha) / 255.0f,
                (float) (totalG / totalAlpha) / 255.0f,
                (float) (totalB / totalAlpha) / 255.0f);
    }
}
