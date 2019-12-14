package de.bonndan.nivio.input;

import de.bonndan.nivio.input.dto.ItemDescription;
import de.bonndan.nivio.input.dto.LandscapeDescription;
import de.bonndan.nivio.model.Group;
import de.bonndan.nivio.model.GroupItem;
import de.bonndan.nivio.model.Groups;
import de.bonndan.nivio.model.LandscapeImpl;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Resolves the groups in the landscape by examining item.group names and adds missing (not pre-configured) groups.
 *
 * Blacklists groups and removes items from the input which are part of a blacklisted group.
 */
public class GroupResolver {

    private final ProcessLog log;

    public GroupResolver(ProcessLog log) {
        this.log = log;
    }

    public void process(LandscapeDescription input, LandscapeImpl landscape) {

        List<Function<String, Boolean>> specs = getSpecs(input.getConfig().getGroupBlacklist());

        input.getGroups().forEach((identifier, groupItem) -> {
            Group g = getGroup(identifier, groupItem);

            if (!isBlacklisted(g.getIdentifier(), specs)){
                log.info("Adding or updating group " + g.getIdentifier());
                landscape.addGroup(g);
            } else {
                log.info("Ignoring blacklisted group " + g.getIdentifier());
            }
        });

        ArrayList<ItemDescription> copy = new ArrayList<>(input.getItemDescriptions());
        copy.forEach(item -> {

            String group = item.getGroup();
            if (StringUtils.isEmpty(item.getGroup())) {
                group = Group.COMMON;
            }

            if (!isBlacklisted(group, specs)) {
                landscape.getGroups().computeIfAbsent(group, s -> getGroup(s, null));
            } else {
                input.getItemDescriptions().remove(item);
            }
        });
    }

    private List<Function<String, Boolean>> getSpecs(List<String> blacklist) {
        return blacklist.stream().map(s -> {
            try {
                Pattern p = Pattern.compile(s);
                return (Function<String, Boolean>) s1 -> p.matcher(s1).matches();
            } catch (Exception e) {
                log.warn("Failed to compile group matcher pattern " + s);
                return (Function<String, Boolean>) s1 -> s1.contains(s);
            }

        }).collect(Collectors.toList());
    }

    private boolean isBlacklisted(String group, List<Function<String, Boolean>> specs) {
        return specs.stream().anyMatch(spec -> spec.apply(group));
    }

    private Group getGroup(String identifier, GroupItem groupItem) {
        Group g = new Group(identifier);
        Groups.merge(g, groupItem);
        return g;
    }

}