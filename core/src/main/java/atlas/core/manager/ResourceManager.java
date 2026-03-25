package atlas.core.manager;

import api.utils.textures.StarLoaderTexture;
import atlas.core.AtlasCore;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;

public class ResourceManager {

	private static final HashMap<String, StarLoaderTexture> textures = new HashMap<>();

	public static void loadResources() {
	}

	public static StarLoaderTexture getTexture(String name) {
		return textures.get(name);
	}

	private static StarLoaderTexture loadTexture(String path) {
		try {
			return StarLoaderTexture.newBlockTexture(ImageIO.read(Objects.requireNonNull(AtlasCore.getInstance().getClass().getClassLoader().getResourceAsStream(path))));
		} catch(IOException exception) {
			AtlasCore.getInstance().logException("Failed to load resource image: " + path, exception);
		}
		return null;
	}
}
