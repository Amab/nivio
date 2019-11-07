package de.bonndan.nivio.input;

import de.bonndan.nivio.input.dto.LandscapeDescription;
import de.bonndan.nivio.input.dto.ItemDescription;
import de.bonndan.nivio.model.*;
import de.bonndan.nivio.notification.NotificationService;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runners.MethodSorters;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static de.bonndan.nivio.model.Items.pick;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class IndexerIntegrationTest {

    @Autowired
    LandscapeRepository landscapeRepository;

    @Mock
    NotificationService notificationService;

    @MockBean
    JavaMailSender mailSender;

    private LandscapeImpl index() {
        return index("/src/test/resources/example/example_env.yml");
    }

    private LandscapeImpl index(String path) {
        File file = new File(getRootPath() + path);
        LandscapeDescription landscapeDescription = LandscapeDescriptionFactory.fromYaml(file);

        Indexer indexer = new Indexer(landscapeRepository, notificationService);

        ProcessLog processLog = indexer.reIndex(landscapeDescription);
        return (LandscapeImpl) processLog.getLandscape();
    }

    @Test //first pass
    public void testIndexing() {
        LandscapeImpl landscape = index();

        Assertions.assertNotNull(landscape);
        assertEquals("mail@acme.org", landscape.getContact());
        Assertions.assertNotNull(landscape.getItems());
        assertEquals(8, landscape.getItems().size());
        Item blog = (Item) Items.pick("blog-server", null, landscape.getItems());
        Assertions.assertNotNull(blog);
        assertEquals(3, blog.getProvidedBy().size());

        Item webserver = (Item) Items.pick("wordpress-web", null, List.copyOf(blog.getProvidedBy()));

        Assertions.assertNotNull(webserver);
        assertEquals(1, webserver.getRelations(RelationType.PROVIDER).size());

        Relation push = (Relation) blog.getRelations().stream()
                .filter(d -> "push".equals(d.getDescription()))
                .findFirst()
                .orElse(null);

        Assertions.assertNotNull(push);

        assertEquals("push", push.getDescription());
        assertEquals("json", push.getFormat());
        assertEquals(blog.getIdentifier(), push.getSource().getIdentifier());
        assertEquals("nivio:example/dashboard/kpi-dashboard", push.getTarget().getFullyQualifiedIdentifier().toString());

        Set<InterfaceItem> interfaces = blog.getInterfaces();
        assertEquals(3, interfaces.size());
        InterfaceItem i =  blog.getInterfaces().stream()
                .filter(d -> d.getDescription().equals("posts"))
                .findFirst()
                .orElseThrow();
        assertEquals("form", i.getFormat());
        assertEquals("http://acme.io/create", i.getUrl().toString());
    }

    @Test //second pass
    public void testReIndexing() {
        LandscapeImpl landscape = index();

        Assertions.assertNotNull(landscape);
        assertEquals("mail@acme.org", landscape.getContact());
        Assertions.assertNotNull(landscape.getItems());
        assertEquals(8, landscape.getItems().size());
        Item blog = (Item) Items.pick("blog-server", null,landscape.getItems());
        Assertions.assertNotNull(blog);
        assertEquals(3, blog.getProvidedBy().size());

        Item webserver = (Item) Items.pick("wordpress-web", null, new ArrayList<LandscapeItem>(blog.getProvidedBy()));
        Assertions.assertNotNull(webserver);
        assertEquals(1, webserver.getRelations(RelationType.PROVIDER).size());

        Relation push = (Relation) blog.getRelations().stream()
                .filter(d -> "push".equals(d.getDescription()))
                .findFirst()
                .orElse(null);

        Assertions.assertNotNull(push);

        assertEquals("push", push.getDescription());
        assertEquals("json", push.getFormat());
        assertEquals("nivio:example/content/blog-server", push.getSource().getFullyQualifiedIdentifier().toString());
        assertEquals("nivio:example/dashboard/kpi-dashboard", push.getTarget().getFullyQualifiedIdentifier().toString());

        Set<InterfaceItem> interfaces = blog.getInterfaces();
        assertEquals(3, interfaces.size());
        InterfaceItem i =  blog.getInterfaces().stream()
                .filter(d -> d.getDescription().equals("posts"))
                .findFirst()
                .orElseThrow();
        assertEquals("form", i.getFormat());
    }

    /**
     * wordpress-web updates must not create new services
     */
    @Test
    public void testIncrementalUpdate() {
        LandscapeImpl landscape = index();
        Item blog = (Item) Items.pick("blog-server", null, landscape.getItems());
        int before = landscape.getItems().size();

        LandscapeDescription landscapeDescription = new LandscapeDescription();
        landscapeDescription.setIdentifier(landscape.getIdentifier());
        landscapeDescription.setIsPartial(true);

        ItemDescription newItem = new ItemDescription();
        newItem.setIdentifier(blog.getIdentifier());
        newItem.setGroup("completelyNewGroup");
        landscapeDescription.getItemDescriptions().add(newItem);

        ItemDescription exsistingWordPress = new ItemDescription();
        exsistingWordPress.setIdentifier("wordpress-web");
        exsistingWordPress.setName("Other name");
        landscapeDescription.getItemDescriptions().add(exsistingWordPress);

        Indexer indexer = new Indexer(landscapeRepository, notificationService);

        //created
        landscape = (LandscapeImpl) indexer.reIndex(landscapeDescription).getLandscape();
        blog = (Item) Items.pick("blog-server", "completelyNewGroup", landscape.getItems());
        assertEquals("completelyNewGroup", blog.getGroup());
        assertEquals(before +1, landscape.getItems().size());

        //updated
        Item wordpress = (Item) Items.pick("wordpress-web", "content", landscape.getItems());
        assertEquals("Other name", wordpress.getName());
        assertEquals("content", wordpress.getGroup());


    }

    /**
     * Ensures that same names in different landscapes do not collide
     */
    @Test
    public void testNameConflictDifferentLandscapes() {
        LandscapeImpl landscape1 = index("/src/test/resources/example/example_env.yml");
        LandscapeImpl landscape2 = index("/src/test/resources/example/example_other.yml");

        Assertions.assertNotNull(landscape1);
        assertEquals("mail@acme.org", landscape1.getContact());
        Assertions.assertNotNull(landscape1.getItems());
        Item blog1 = (Item) Items.pick("blog-server", null,landscape1.getItems());
        Assertions.assertNotNull(blog1);
        assertEquals("blog", blog1.getShortName());

        Assertions.assertNotNull(landscape2);
        assertEquals("nivio:other", landscape2.getIdentifier());
        assertEquals("mail@other.org", landscape2.getContact());
        Assertions.assertNotNull(landscape2.getItems());
        Item blog2 = (Item) Items.pick("blog-server", null,landscape2.getItems());
        Assertions.assertNotNull(blog2);
        assertEquals("blog1", blog2.getShortName());
    }

    /**
     * Ensures that same names in different landscapes do not collide
     */
    @Test
    public void testDataflow() {
        LandscapeImpl landscape1 = index("/src/test/resources/example/example_dataflow.yml");

        Assertions.assertNotNull(landscape1);
        Assertions.assertNotNull(landscape1.getItems());
        Item blog1 = (Item) Items.pick("blog-server", "content1",landscape1.getItems());
        Assertions.assertNotNull(blog1);
        Item blog2 = (Item) Items.pick("blog-server", "content2",landscape1.getItems());
        Assertions.assertNotNull(blog2);
        assertEquals("Demo Blog", blog1.getName());
        assertEquals(
                FullyQualifiedIdentifier.build("nivio:dataflowtest", "content1", "blog-server").toString(),
                blog1.toString()
        );

        assertNotNull(blog1.getRelations());
        assertEquals(2, blog1.getRelations().size());
    }

    @Test
    public void environmentTemplatesApplied() {
        LandscapeImpl landscape = index("/src/test/resources/example/example_templates.yml");

        LandscapeItem web = pick( "web", null, landscape.getItems());
        Assert.assertNotNull(web);
        assertEquals("web", web.getIdentifier());
        assertEquals("webservice", web.getType());
    }

    @Test
    public void readGroups() {
        LandscapeImpl landscape1 = index("/src/test/resources/example/example_env.yml");
        Map<String, GroupItem> groups = landscape1.getGroups();
        assertTrue(groups.containsKey("content"));
        Group content = (Group) groups.get("content");
        assertFalse(content.getItems().isEmpty());
        assertEquals(3, content.getItems().size());

        assertTrue(groups.containsKey("ingress"));
        Group ingress = (Group) groups.get("ingress");
        assertFalse(ingress.getItems().isEmpty());
        assertEquals(1, ingress.getItems().size());
    }

    @Test
    public void readGroupsContains() {
        LandscapeImpl landscape1 = index("/src/test/resources/example/example_groups.yml");
        Group a = (Group) landscape1.getGroups().get("groupA");
        assertNotNull(pick("blog-server", null, a.getItems()));
        assertNotNull(pick("crappy_dockername-234234", null, a.getItems()));
    }

    private String getRootPath() {
        Path currentRelativePath = Paths.get("");
        return currentRelativePath.toAbsolutePath().toString();
    }
}
