package de.bonndan.nivio.output.layout;

import de.bonndan.nivio.model.Group;
import de.bonndan.nivio.model.Item;
import de.bonndan.nivio.model.Landscape;
import de.bonndan.nivio.model.LandscapeItem;
import de.bonndan.nivio.output.LayoutedArtifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders a graph of group containers only, not regarding items inside the containers.
 */
public class AllGroupsGraph implements LayoutedArtifact<ComponentBounds> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AllGroupsGraph.class);

    private final Map<Group, ComponentBounds> groupNodes = new LinkedHashMap<>();
    private final FastOrganicLayout layout;
    private final Landscape landscape;

    public AllGroupsGraph(Landscape landscape, Map<String, Group> groups, Map<String, SubGraph> subgraphs) {
        this.landscape = landscape;

        LOGGER.debug("Subgraphs sequence: {}", subgraphs);

        List<LandscapeItem> items = new ArrayList<>();
        groups.forEach((groupName, groupItem) -> {
            ComponentBounds groupGeometry = subgraphs.get(groupName).getOuterBounds();
            groupNodes.put(groupItem, groupGeometry);
            items.addAll(groupItem.getItems());
        });
        LOGGER.debug("Group node sequence: {}", groupNodes);

        addVirtualEdgesBetweenGroups(items);

        layout = new FastOrganicLayout(new ArrayList<>(groupNodes.values()));
        layout.setDebug(true);

        //Optional.ofNullable(config.getJgraphx().getMaxIterations())
        //        .ifPresent(layout::setMaxIterations);

        //Optional.ofNullable(config.getJgraphx().getMinDistanceLimitFactor())
        //        .ifPresent(f -> layout.setMinDistanceLimit(layout.getMinDistanceLimit() * f));

        layout.setForceConstant(250);
        layout.setMaxDistanceLimit(1000);
        layout.execute();
        LOGGER.info("AllGroupsGraph bounds: {}", layout.getBounds());
    }


    /**
     * Virtual edges between group containers enable organic layout of groups.
     */
    private void addVirtualEdgesBetweenGroups(List<LandscapeItem> items) {

        GroupConnections groupConnections = new GroupConnections();

        items.forEach(item -> {
            final String group;
            if (StringUtils.isEmpty(item.getGroup())) {
                LOGGER.warn("Item {} has no group, using " + Group.COMMON, item);
                group = Group.COMMON;
            } else {
                group = item.getGroup();
            }
            ComponentBounds groupNode = findGroupBounds(group);

            item.getRelations().forEach(relationItem -> {
                Item targetItem = (Item) relationItem.getTarget();
                if (targetItem == null) {
                    LOGGER.warn("Virtual connections: No target in relation item {}", relationItem);
                    return;
                }

                String targetGroup = targetItem.getGroup() == null ? Group.COMMON : targetItem.getGroup();
                ComponentBounds targetGroupNode = findGroupBounds(targetGroup);

                if (groupConnections.canConnect(group, targetGroup)) {
                    groupNode.getOpposites().add(targetGroupNode.getComponent());
                    targetGroupNode.getOpposites().add(groupNode.getComponent());
                    groupConnections.connect(group, targetGroup, "Virtual connection between ");
                }
            });
        });
    }

    private ComponentBounds findGroupBounds(String group) {

        if (StringUtils.isEmpty(group))
            group = Group.COMMON;

        String finalGroup = group;
        return groupNodes.entrySet().stream()
                .filter(entry -> finalGroup.equals(entry.getKey().getIdentifier()))
                .findFirst().map(Map.Entry::getValue).orElseThrow(() -> new RuntimeException("Group " + finalGroup + " not found."));
    }

    /**
     * For layout debugging.
     */
    public ComponentBounds getRendered() {
        return layout.getOuterBounds(landscape);
    }

}
