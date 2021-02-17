package de.bonndan.nivio.observation;

import de.bonndan.nivio.input.*;
import de.bonndan.nivio.input.dto.LandscapeDescription;
import de.bonndan.nivio.model.Landscape;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service to register landscapes to observe description source changes.
 */
@Service
public class ObserverRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(ObserverRegistry.class);

    private final Map<String, LandscapeObserverPool> observerMap = new ConcurrentHashMap<>();

    private final LandscapeObserverPoolFactory landscapeObserverPoolFactory;
    private final ThreadPoolTaskScheduler taskScheduler;
    private final IndexingDispatcher indexingDispatcher;

    public ObserverRegistry(LandscapeObserverPoolFactory landscapeObserverPoolFactory,
                            ThreadPoolTaskScheduler taskScheduler,
                            IndexingDispatcher indexingDispatcher
    ) {
        this.landscapeObserverPoolFactory = landscapeObserverPoolFactory;
        this.taskScheduler = taskScheduler;
        this.indexingDispatcher = indexingDispatcher;
    }

    /**
     * Landscape are registered for observation here.
     * <p>
     * On processing success, {@link ProcessingFinishedEvent} is fired and read here to register the landscape.
     */
    @EventListener(ProcessingFinishedEvent.class)
    public void onProcessingFinishedEvent(ProcessingFinishedEvent event) {
        LandscapeDescription landscapeDescription = (LandscapeDescription) event.getSource();
        Landscape landscape = Objects.requireNonNull(event.getLandscape());

        if (landscapeDescription == null) {
            String msg = "No landscape description (input) available. Landscape " + landscape.getIdentifier() + " could not be registered for observation";
            landscape.getLog().warn(msg);
            LOGGER.warn(msg);
            return;
        }

        LandscapeObserverPool pool = observerMap.computeIfAbsent(landscape.getIdentifier(), s -> {
            LOGGER.info("Registered landscape {} for observation.", landscapeDescription);
            return new LandscapeObserverPool(taskScheduler, 30 * 1000);
        });
        pool.updateObservers(landscapeObserverPoolFactory.getObserversFor(landscape, landscapeDescription));
    }

    @EventListener(InputChangedEvent.class)
    public void onInputChangedEvent(InputChangedEvent event) {
        ObservedChange observedChange = event.getSource();
        Landscape landscape = observedChange.getLandscape();
        LOGGER.info("Observed change in landscape {}: {}", landscape.getIdentifier(), String.join("; ", observedChange.getChanges()));
        indexingDispatcher.fromIncoming(landscape);
        LOGGER.info("Triggered new IndexingEvent for landscape {}", landscape.getIdentifier());
    }

    /**
     * @return the currently observed landscapes.
     */
    Set<String> getObservedLandscapes() {
        return observerMap.keySet();
    }

}
