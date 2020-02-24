package de.bonndan.nivio.output.map.svg;

import j2html.tags.DomContent;

import static j2html.TagCreator.rawHtml;

class SVGPattern extends Component {

    private final String id, link;
    private final int size;

    SVGPattern(String id, String link, int size) {
        this.id = id;
        this.link = link;
        this.size = size;
    }


    public DomContent render() {

        return rawHtml(
                "<defs>" +
                        "<pattern id=\"" + id + "\" patternUnits=\"objectBoundingBox\" x =\"0\" y=\"0\" width=\"" + size + "\" height=\"" + size + "\" >" +
                        "<rect height=\"100\" width=\"100\" fill=\"white\" />" +
                        "<image xlink:href=\"" + link + "\" width=\"" + size * 2 + "\" height=\"" + size * 2 + "\"  />" +
                        "</pattern>" +
                        "</defs >"
        );
    }
}

