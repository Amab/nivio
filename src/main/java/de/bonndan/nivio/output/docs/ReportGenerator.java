package de.bonndan.nivio.output.docs;

import de.bonndan.nivio.assessment.Assessment;
import de.bonndan.nivio.model.Group;
import de.bonndan.nivio.model.Landscape;
import de.bonndan.nivio.output.Color;
import de.bonndan.nivio.output.LocalServer;
import de.bonndan.nivio.output.icons.IconService;
import de.bonndan.nivio.output.map.MapController;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.Map;
import java.util.Objects;

import static de.bonndan.nivio.output.FormatUtils.nice;
import static de.bonndan.nivio.output.map.MapController.MAP_SVG_ENDPOINT;
import static j2html.TagCreator.*;
import static org.springframework.util.StringUtils.isEmpty;

/**
 * Generates a report containing all landscape groups and items.
 *
 *
 */
public class ReportGenerator extends HtmlGenerator {

    public ReportGenerator(LocalServer localServer, IconService iconService) {
        super(localServer, iconService);
    }

    @Override
    public String toDocument(@NonNull final Landscape landscape, @NonNull final Assessment assessment, @Nullable final SearchConfig searchConfig) {
        return writeLandscape(Objects.requireNonNull(landscape), Objects.requireNonNull(assessment));
    }

    private String writeLandscape(final Landscape landscape, final Assessment assessment) {

        return html(
                getHead(landscape),
                body(
                        h1(landscape.getName()),
                        iff(!isEmpty(landscape.getContact()), p("Contact: " + nice(landscape.getContact()))),
                        div(embed().attr("src", MapController.PATH + "/" + landscape.getIdentifier() + "/" + MAP_SVG_ENDPOINT).attr("class", "img-fluid img-thumbnail mx-auto d-block")),
                        br(), br(),
                        rawHtml(writeGroups(landscape, assessment))
                )
        ).renderFormatted();
    }

    private String writeGroups(Landscape landscape, Assessment assessment) {
        final StringBuilder builder = new StringBuilder();
        Map<String, Group> groups = landscape.getGroups();
        groups.forEach((s, groupItem) -> {
            String color = "#" + Color.getGroupColor(groupItem);
            builder.append(
                    h2(rawHtml("Group: " + "<span style=\"color: " + color + "\">" + GROUP_CIRCLE + "</span> " + s))
                            .attr("class", "rounded").render()
            );
            builder.append(
                    div().attr("class", "group")
                            .with(groupItem.getItems().stream().map(item -> this.writeItem(item, assessment)))
                            .render()
            );
        });

        return builder.toString();
    }

}
