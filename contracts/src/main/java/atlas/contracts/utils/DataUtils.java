package atlas.contracts.utils;

import api.common.GameCommon;
import api.mod.ModSkeleton;
import atlas.contracts.AtlasContracts;

public class DataUtils {

	public static String getWorldDataPath() {
		return getResourcesPath() + "/data/" + GameCommon.getUniqueContextId();
	}

	public static String getResourcesPath() {
		ModSkeleton skeleton = AtlasContracts.getInstance().getSkeleton();
		return skeleton.getResourcesFolder().getPath().replace('\\', '/');
	}
}
