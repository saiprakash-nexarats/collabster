package com.collabsphere_first.interaction.adapter;

import com.collabsphere_first.common.content.ContentType;
import com.collabsphere_first.interaction.exception.InvalidInteractionException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class ContentInteractionAdapterRegistry {

    private final Map<ContentType, ContentInteractionAdapter> adapters = new EnumMap<>(ContentType.class);

    public ContentInteractionAdapterRegistry(List<ContentInteractionAdapter> adapterList) {
        if (adapterList != null) {
            for (ContentInteractionAdapter adapter : adapterList) {
                adapters.put(adapter.supports(), adapter);
            }
        }
    }

    public ContentInteractionAdapter getAdapter(ContentType type) {
        ContentInteractionAdapter adapter = adapters.get(type);
        if (adapter == null) {
            throw new InvalidInteractionException("No interaction adapter registered for content type: " + type);
        }
        return adapter;
    }
}
