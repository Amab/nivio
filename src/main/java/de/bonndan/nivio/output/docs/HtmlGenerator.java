package de.bonndan.nivio.output.docs;

import de.bonndan.nivio.assessment.Assessment;
import de.bonndan.nivio.assessment.StatusValue;
import de.bonndan.nivio.model.Item;
import de.bonndan.nivio.model.Label;
import de.bonndan.nivio.model.Labeled;
import de.bonndan.nivio.model.Landscape;
import de.bonndan.nivio.output.Color;
import de.bonndan.nivio.output.FormatUtils;
import de.bonndan.nivio.output.LocalServer;
import de.bonndan.nivio.output.icons.IconService;
import j2html.tags.ContainerTag;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static de.bonndan.nivio.model.Relation.DELIMITER;
import static de.bonndan.nivio.output.FormatUtils.ifPresent;
import static de.bonndan.nivio.output.FormatUtils.nice;
import static j2html.TagCreator.*;
import static org.springframework.util.StringUtils.isEmpty;


public abstract class HtmlGenerator {

    protected static final String GROUP_CIRCLE = "&#10687;";

    @NonNull
    protected final LocalServer localServer;

    @NonNull
    protected final IconService iconService;

    /**
     * Generates the HTML report.
     *
     * @param landscape    the landscape object
     * @param assessment   a landscape assessment
     * @param searchConfig configuration for the report
     * @return rendered html
     */
    public abstract String toDocument(@NonNull final Landscape landscape, @NonNull final Assessment assessment, @Nullable final SearchConfig searchConfig);

    protected HtmlGenerator(@NonNull final LocalServer localServer, @NonNull final IconService iconService) {
        this.localServer = Objects.requireNonNull(localServer);
        this.iconService = Objects.requireNonNull(iconService);
    }

    protected ContainerTag getHead(Landscape landscape) {

        URL css = localServer.getUrl("/css/bootstrap.min.css").orElse(null);
        return head(
                title(landscape.getName()),
                link().condAttr(css != null, "rel", "stylesheet").attr("href", css),
                meta().attr("charset", "utf-8"),
                meta().attr("name", "viewport").attr("content", "width=device-width, initial-scale=1, shrink-to-fit=no"),
                meta().attr("name", "description").attr("content", landscape.getName()),
                meta().attr("name", "author").attr("content", landscape.getContact()),
                meta().attr("generator", "author").attr("content", "nivio"),
                style("html {margin: 1rem} .group{margin-top: 1rem;} .card{margin-bottom: 1rem;}").attr("type", "text/css")
        );
    }

    protected ContainerTag writeItem(Item item, Assessment assessment) {
        boolean hasRelations = !item.getRelations().isEmpty();
        boolean hasInterfaces = !item.getInterfaces().isEmpty();
        String groupColor = "#" + Color.nameToRGB(item.getGroup(), Color.GRAY);

        List<ContainerTag> links = item.getLinks().entrySet().stream()
                .map(stringURLEntry -> a(" " + stringURLEntry.getKey()).attr("href", stringURLEntry.getValue().toString()))
                .collect(Collectors.toList());


        List<ContainerTag> labelList = getLabelList(item);

        List<String> frameworks = Labeled.withPrefix(Label.framework.name(), item.getLabels()).entrySet().stream()
                .map(mapEntry -> String.format("%s: %s", StringUtils.capitalize(Label.framework.unprefixed(mapEntry.getKey())), mapEntry.getValue()))
                .collect(Collectors.toList());

        List<StatusValue> statusValues = assessment.getResults().get(item.getFullyQualifiedIdentifier());

        return div(
                div(
                        iff(!isEmpty(item.getLabel(Label.note)), div(item.getLabel(Label.note)).attr("class", "alert alert-warning float float-right")),
                        a().attr("id", item.getFullyQualifiedIdentifier().toString()),
                        h3(
                                img().attr("src", iconService.getIconUrl(item)).attr("width", "30px").attr("class", "img-fluid"),
                                rawHtml(" "),
                                rawHtml(isEmpty(item.getName()) ? item.getIdentifier() : item.getName())
                        ),
                        p(FormatUtils.nice(item.getDescription())),


                        ul().with(
                                iff(!isEmpty(item.getName()), li("Name: " + FormatUtils.nice(item.getName())))
                                , iff(!isEmpty(item.getFullyQualifiedIdentifier().toString()), li("Full identifier: " + item.getFullyQualifiedIdentifier()))
                                , iff(!isEmpty(item.getIdentifier()), li("Identifier: " + item.getIdentifier()))
                                , iff(!isEmpty(item.getGroup()), li(rawHtml("Group: " + "<span style=\"color: " + groupColor + "\">" + GROUP_CIRCLE + "</span> " + FormatUtils.nice(item.getGroup()))))
                                , iff(!isEmpty(item.getContact()), li("Contact: " + FormatUtils.nice(item.getContact())))
                                , iff(!isEmpty(item.getOwner()), li("Owner: " + FormatUtils.nice(item.getOwner())))
                                , iff(!isEmpty(item.getType()), li("Type: " + item.getType()))
                                , iff(links.size() > 1, li("Links: ").with(links))
                                , iff(frameworks.size() > 0, li("Frameworks: " + String.join(String.format("%s ", DELIMITER), frameworks)))
                        ).with(labelList),


                        //statuses
                        iff(!statusValues.isEmpty(), h4("Status information")),
                        dl().with(
                                statusValues.stream().map(statusItem ->
                                        join(
                                                dt(FormatUtils.nice(
                                                        statusItem.getField().endsWith("." + item.getIdentifier())
                                                                ? statusItem.getField().replace("." + item.getIdentifier(), "")
                                                                : statusItem.getField()
                                                        ) + " "
                                                ).with(
                                                        span(" " + statusItem.getStatus() + " ")
                                                                .attr("class", "badge")
                                                                .attr("style", "background-color: " + statusItem.getStatus() + " !important")
                                                ),
                                                iff(
                                                        !isEmpty(statusItem.getMessage()) && !"summary".equals(statusItem.getMessage()),
                                                        dd(span(" " + FormatUtils.nice(statusItem.getMessage())))
                                                )
                                        )
                                )
                        ),


                        //data flow
                        iff(hasRelations, h4("Relations")),
                        iff(hasRelations, ul().with(
                                item.getRelations().stream()
                                        .map(df -> {

                                            String direction = (df.getSource().equals(item)) ?
                                                    " &#10142; " : " incoming from ";
                                            Item end = (df.getSource().equals(item)) ?
                                                    df.getTarget() : df.getSource();

                                            return li(rawHtml((df.getType() != null ? df.getType() : "") + " "
                                                            + ifPresent(df.getFormat()) + " "
                                                            + ifPresent(df.getDescription())
                                                            + direction),
                                                    a(end.toString()).attr("href", "#" + end.getFullyQualifiedIdentifier()));
                                        })
                                )
                        ),

                        //interfaces
                        iff(hasInterfaces, h4("Interfaces")),
                        iff(hasInterfaces, ul().with(
                                item.getInterfaces().stream().map(interfaceItem -> li(
                                        span(interfaceItem.getDescription()),
                                        iff(!StringUtils.isEmpty(interfaceItem.getFormat()), span(", format: " + interfaceItem.getFormat())),
                                        iff(interfaceItem.getUrl() != null && !StringUtils.isEmpty(interfaceItem.getUrl().toString()),
                                                span(", ").with(a(interfaceItem.getUrl().toString()).attr("href", interfaceItem.getUrl().toString()))
                                        )
                                ))
                        ))
                ).attr("class", "card-body")

        ).attr("class", "card");
    }

    protected List<ContainerTag> getLabelList(Item item) {
        Function<Map.Entry<String, String>, Boolean> filter = s -> {
            if (isEmpty(s.getValue())) {
                return false;
            }
            if (Label.type.name().equals(s.getKey())) {
                return false;
            }
            if (Label.framework.name().equals(s.getKey())) {
                return false;
            }
            if (Label.icon.name().equals(s.getKey())) {
                return false;
            }
            if (Label.color.name().equals(s.getKey())) {
                return false;
            }
            if (Label.fill.name().equals(s.getKey())) {
                return false;
            }
            //filter out statuses, they are part of the assessment
            return !s.getKey().startsWith(Label.status.name());
        };

        return Labeled.groupedByPrefixes(item.getLabels()).entrySet().stream()
                .filter(filter::apply)
                .map(mapEntry -> {
                    String key = StringUtils.capitalize(mapEntry.getKey());
                    if (key.equals(StringUtils.capitalize(Label.shortname.name()))) {
                        key = Label.shortname.meaning;
                    }

                    return li(String.format("%s: %s", key, nice(mapEntry.getValue().replace(Labeled.PREFIX_VALUE_DELIMITER, " "))));
                })
                .collect(Collectors.toList());
    }
}
