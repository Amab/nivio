package de.bonndan.nivio.input;

import de.bonndan.nivio.input.dto.ItemDescription;
import de.bonndan.nivio.input.dto.LandscapeDescription;
import de.bonndan.nivio.model.*;
import de.bonndan.nivio.search.ItemMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Compares the input {@link LandscapeDescription} against the existing {@link Landscape}.
 *
 * Adds, updates and removes items in the landscape.
 */
public class DiffProcessor extends Processor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiffProcessor.class);

    protected DiffProcessor(ProcessLog processLog) {
        super(processLog);
    }

    @Override
    public void process(LandscapeDescription input, Landscape landscape) {
        Set<Item> existingItems = landscape.getItems().all();

        //insert new ones
        List<ItemDescription> newItems = added(input.getItemDescriptions().all(), existingItems, landscape);
        Set<Item> inLandscape = new HashSet<>();
        processLog.info(String.format("Adding %d items in env %s", newItems.size(), landscape.getIdentifier()));
        newItems.forEach(
                newItem -> {
                    processLog.info(String.format("Creating new item %s in env %s", newItem.getIdentifier(), input.getIdentifier()));
                    inLandscape.add(ItemFactory.fromDescription(newItem, landscape));
                }
        );

        //update existing
        List<Item> kept;
        if (input.isPartial()) {
            kept = new ArrayList<>(existingItems); //we want to keep all, increment does not contain all items
        } else {
            kept = kept(input.getItemDescriptions().all(), existingItems, landscape);
        }
        processLog.info(String.format("Updating %d items in landscape %s", kept.size(), landscape.getIdentifier()));
        kept.forEach(
                item -> {

                    ItemDescription description = input.getItemDescriptions().find(ItemMatcher.forTarget(item)).orElse(null);
                    if (description == null) {
                        if (input.isPartial()) {
                            inLandscape.add(item);
                            return;
                        } else {
                            throw new ProcessingException(input, "Item not found " + item.getIdentifier());
                        }
                    }

                    processLog.info("Updating item " + item.getIdentifier() + " in landscape " + input.getIdentifier());

                    inLandscape.add(ItemFactory.assignAll(item, description));
                }
        );

        //cleanup
        landscape.setItems(inLandscape); //this already removes the items from the landscape.items

        //remove references left over in groups
        List<Item> toDelete = getUnreferenced(input, inLandscape, existingItems, processLog);
        toDelete.forEach(item -> {
            processLog.info("Removing item " + item + " from landscape");
            landscape.getGroup(item.getGroup()).ifPresent(group -> {
                boolean removed = group.removeItem(item);
                if (!removed) {
                    LOGGER.warn("Failed to remove item {}", item);
                }
            });
        });
    }

    private List<Item> getUnreferenced(
            final LandscapeDescription landscapeDescription,
            Set<Item> kept,
            Set<Item> all,
            ProcessLog logger
    ) {
        if (landscapeDescription.isPartial()) {
            logger.info("Incremental change, will not remove any unreferenced items.");
            return new ArrayList<>();
        }

        return removed(kept, all);
    }

    /**
     * Returns all items that are also present in the new itemDescriptions
     */
    static List<Item> kept(Collection<? extends ItemDescription> newItems, Collection<? extends Item> items, Landscape landscape) {
        return items.stream()
                .filter(item -> presentInNewItems(item, newItems, landscape))
                .collect(Collectors.toList());
    }

    /**
     * Returns all elements removed from the second list.
     */
    static List<Item> removed(Collection<Item> items, Collection<Item> itemDescriptions) {
        return itemDescriptions.stream()
                .filter(item -> doesNotExistAsItem(item, items))
                .collect(Collectors.toList());
    }

    /**
     * Returns all elements which are not in the second list
     *
     * @return
     */
    static List<ItemDescription> added(Collection<ItemDescription> itemDescriptions, Collection<Item> existingItems, Landscape landscape) {
        return itemDescriptions.stream()
                .filter(newItem -> doesNotExistAsItem(newItem, existingItems))
                .collect(Collectors.toList());
    }

    private static boolean presentInNewItems(final Item item, Collection<? extends ItemDescription> newItems, Landscape landscape) {
        return newItems.stream()
                .anyMatch(inList -> ItemMatcher.forTarget(item).isSimilarTo(inList.getFullyQualifiedIdentifier()));
    }

    private static boolean doesNotExistAsItem(final ItemDescription item, Collection<? extends Component> items) {
        return items.stream().noneMatch(
                inList -> ItemMatcher.forTarget(item.getFullyQualifiedIdentifier()).isSimilarTo(inList.getFullyQualifiedIdentifier())
        );
    }

    private static boolean doesNotExistAsItem(final Item item, Collection<? extends Component> items) {
        return items.stream().noneMatch(
                inList -> ItemMatcher.forTarget(item).isSimilarTo(inList.getFullyQualifiedIdentifier())
        );
    }
}
