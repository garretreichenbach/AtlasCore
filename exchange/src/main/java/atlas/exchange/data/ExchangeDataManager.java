package atlas.exchange.data;

import api.mod.config.PersistentObjectUtil;
import atlas.core.AtlasCore;
import atlas.core.data.DataManager;

import java.util.*;

/**
 * Manages exchange listing data for all players.
 *
 * @author TheDerpGamer
 */
public class ExchangeDataManager extends DataManager<ExchangeData> {

    private final Set<ExchangeData> clientCache = new HashSet<>();
    private static ExchangeDataManager serverInstance;
    private static ExchangeDataManager clientInstance;

    public static ExchangeDataManager getInstance(boolean server) {
        if(server) {
            if(serverInstance == null) serverInstance = new ExchangeDataManager();
            return serverInstance;
        } else {
            if(clientInstance == null) {
                clientInstance = new ExchangeDataManager();
                clientInstance.requestFromServer();
            }
            return clientInstance;
        }
    }

    @Override
    public Set<ExchangeData> getServerCache() {
        List<Object> objects = PersistentObjectUtil.getObjects(AtlasCore.getInstance().getSkeleton(), ExchangeData.class);
        Set<ExchangeData> data = new HashSet<>();
        for(Object object : objects) data.add((ExchangeData) object);
        return data;
    }

    @Override
    public String getDataTypeName() {
        return "EXCHANGE_DATA";
    }

    @Override
    public Set<ExchangeData> getClientCache() {
        return Collections.unmodifiableSet(clientCache);
    }

    @Override
    public void addToClientCache(ExchangeData data) {
        clientCache.add(data);
    }

    @Override
    public void removeFromClientCache(ExchangeData data) {
        clientCache.remove(data);
    }

    @Override
    public void updateClientCache(ExchangeData data) {
        clientCache.remove(data);
        clientCache.add(data);
    }

    @Override
    public void createMissingData(Object... args) {
        // Exchange listings don't need per-player initialization
    }

    public static Set<ExchangeData> getByCategory(ExchangeData.ExchangeDataCategory category) {
        Set<ExchangeData> data = new HashSet<>();
        for(ExchangeData exchangeData : getInstance(false).getClientCache()) {
            if(exchangeData.getCategory() == category) data.add(exchangeData);
        }
        return data;
    }

    public boolean existsName(String name) {
        for(ExchangeData data : clientCache) {
            if(data.getName().toLowerCase(Locale.ENGLISH).trim().equals(name.toLowerCase(Locale.ENGLISH).trim())) {
                return true;
            }
        }
        return false;
    }
}
