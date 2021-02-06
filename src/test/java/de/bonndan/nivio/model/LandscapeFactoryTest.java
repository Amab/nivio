package de.bonndan.nivio.model;

import de.bonndan.nivio.input.dto.LandscapeDescription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class LandscapeFactoryTest {

    private LandscapeDescription description;

    @BeforeEach
    public void setup() throws MalformedURLException {
        description = new LandscapeDescription("foo", "bar", "baz@mail.com");
        description.setSource("/my/file.yaml");
        description.setOwner("baz");
        description.setDescription("Hello, World.");
        description.setLink("home", new URL("https://dedica.team"));
        description.getLabels().put("one", "two");
    }

    @Test
    public void create() {
        Landscape landscape = LandscapeFactory.createFromInput(description);
        assertNotNull(landscape);
        assertEquals(description.getIdentifier(), landscape.getIdentifier());
        assertEquals(description.getSource(), landscape.getSource());
    }

    @Test
    public void createWithMinIdentifier() {
        Landscape landscape = LandscapeFactory.createForTesting("l1", "l1Landscape").build();
        assertNotNull(landscape);
    }

    @Test
    public void createAddsCommonGroup() {
        Landscape landscape = LandscapeFactory.createFromInput(description);
        assertNotNull(landscape);
        assertEquals(1, landscape.getGroups().size());
        assertNotNull(landscape.getGroup(Group.COMMON));
    }

    @Test
    public void createFromInput() {
        Landscape landscape = LandscapeFactory.createFromInput(description);

        assertEquals(description.getContact(), landscape.getContact());
        assertEquals(description.getConfig(), landscape.getConfig());
        assertEquals(description.getOwner(), landscape.getOwner());
        assertEquals(description.getDescription(), landscape.getDescription());
        assertEquals(description.getName(), landscape.getName());
        assertEquals(1,  landscape.getLabels().size());
        assertEquals("two",  landscape.getLabels().get("one"));
        assertEquals(1,  landscape.getLinks().size());
        assertEquals("https://dedica.team",  landscape.getLinks().get("home").getHref().toString());
    }

    @Test
    public void recreateFromExisting() {

        Landscape existing = LandscapeBuilder.aLandscape()
                .withIdentifier(description.getIdentifier())
                .withName("A Test")
                .withContact("foo")
                .withOwner("bar")
                .withGroups(Map.of("agroup", new Group("agroup")))
                .withItems(Set.of(new Item("agroup", "hihi")))
                .build();

        //when
        Landscape landscape = LandscapeFactory.recreate(existing, description);

        //then
        assertThat(landscape.getContact()).isEqualTo(description.getContact());
        assertEquals(description.getConfig(), landscape.getConfig());
        assertEquals(description.getOwner(), landscape.getOwner());
        assertEquals(description.getDescription(), landscape.getDescription());
        assertEquals(description.getName(), landscape.getName());
        assertEquals(1,  landscape.getLabels().size());
        assertEquals("two",  landscape.getLabels().get("one"));
        assertEquals(1,  landscape.getLinks().size());
        assertEquals("https://dedica.team",  landscape.getLinks().get("home").getHref().toString());
    }

}