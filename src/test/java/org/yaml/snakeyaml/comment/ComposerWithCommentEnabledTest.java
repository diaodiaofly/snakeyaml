package org.yaml.snakeyaml.comment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import org.junit.Test;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.comments.CommentLine;
import org.yaml.snakeyaml.composer.Composer;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.events.Event;
import org.yaml.snakeyaml.events.Event.ID;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;
import org.yaml.snakeyaml.parser.Parser;
import org.yaml.snakeyaml.parser.ParserImpl;
import org.yaml.snakeyaml.reader.StreamReader;
import org.yaml.snakeyaml.resolver.Resolver;

public class ComposerWithCommentEnabledTest {

    @SuppressWarnings("unused")
    private void assertEventListEquals(List<ID> expectedEventIdList, Parser parser) {
        for (ID expectedEventId : expectedEventIdList) {
            parser.checkEvent(expectedEventId);
            Event event = parser.getEvent();
            System.out.println(event);
            if (event == null) {
                fail("Missing event: " + expectedEventId);
            }
            assertEquals(expectedEventId, event.getEventId());
        }
    }

    private void printBlockComment(Node node, int level, PrintStream out) {
        if (node.getBlockComments() != null) {
            List<CommentLine> blockComments = node.getBlockComments();
            for (int i = 0; i < blockComments.size(); i++) {
                printWithIndent("Block Comment", level, out);
            }
        }
    }

    private void printEndComment(Node node, int level, PrintStream out) {
        if (node.getEndComments() != null) {
            List<CommentLine> endComments = node.getEndComments();
            for (int i = 0; i < endComments.size(); i++) {
                printWithIndent("End Comment", level, out);
            }
        }
    }

    private void printInLineComment(Node node, int level, PrintStream out) {
        if (node.getInLineComments() != null) {
            List<CommentLine> inLineComments = node.getInLineComments();
            for (int i = 0; i < inLineComments.size(); i++) {
                printWithIndent("InLine Comment", level + 1, out);
            }
        }
    }

    private void printWithIndent(String line, int level, PrintStream out) {
        for (int ix = 0; ix < level; ix++) {
            out.print("    ");
        }
        out.println(line);
    }

    private void printNodeInternal(Node node, int level, PrintStream out) {

        if (node instanceof MappingNode) {
            MappingNode mappingNode = (MappingNode) node;
            printBlockComment(mappingNode, level, out);
            printWithIndent(mappingNode.getClass().getSimpleName(), level, out);
            for (NodeTuple childNodeTuple : mappingNode.getValue()) {
                printWithIndent("Tuple", level + 1, out);
                printNodeInternal(childNodeTuple.getKeyNode(), level + 2, out);
                printNodeInternal(childNodeTuple.getValueNode(), level + 2, out);
            }
            printInLineComment(mappingNode, level, out);
            printEndComment(mappingNode, level, out);

        } else if (node instanceof SequenceNode) {
            SequenceNode sequenceNode = (SequenceNode) node;
            printBlockComment(sequenceNode, level, out);
            printWithIndent(sequenceNode.getClass().getSimpleName(), level, out);
            for (Node childNode : sequenceNode.getValue()) {
                printNodeInternal(childNode, level + 1, out);
            }
            printInLineComment(sequenceNode, level, out);
            printEndComment(sequenceNode, level, out);

        } else if (node instanceof ScalarNode) {
            ScalarNode scalarNode = (ScalarNode) node;
            printBlockComment(scalarNode, level, out);
            printWithIndent(scalarNode.getClass().getSimpleName() + ": " + scalarNode.getValue(), level, out);
            printInLineComment(scalarNode, level, out);
            printEndComment(scalarNode, level, out);

        } else {
            printBlockComment(node, level, out);
            printWithIndent(node.getClass().getSimpleName(), level, out);
            printInLineComment(node, level, out);
            printEndComment(node, level, out);
        }
    }

    private void printNodeList(List<Node> nodeList) {
        System.out.println("BEGIN");
        for (Node node : nodeList) {
            printNodeInternal(node, 1, System.out);
        }
        System.out.println("DONE\n");
    }

    private List<Node> getNodeList(Composer composer) {
        List<Node> nodeList = new ArrayList<>();
        while (composer.checkNode()) {
            nodeList.add(composer.getNode());
        }
        return nodeList;
    }

    private void assertNodesEqual(String[] expecteds, List<Node> nodeList) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintStream out = new PrintStream(baos)) {
            for (Node node : nodeList) {
                printNodeInternal(node, 0, out);
            }
        }
        String actualString = baos.toString();
        String[] actuals = actualString.split("\n");
        for(int ix = 0; ix < Math.min(expecteds.length, actuals.length); ix++) {
            assertEquals(expecteds[ix], actuals[ix]);
        }
        assertEquals(expecteds.length, actuals.length);
    }

    public Composer newComposerWithCommentsEnabled(String data) {
        return new Composer(new ParserImpl(new StreamReader(data), true), new Resolver());
    }

    @Test
    public void testEmpty() {
        String data = "";
        String[] expecteds = new String[] { //
                "" //
        };

        Composer sut = newComposerWithCommentsEnabled(data);
        List<Node> result = getNodeList(sut);

        printNodeList(result);
        assertNodesEqual(expecteds, result);
    }

    @Test
    public void testParseWithOnlyComment() {
        String data = "# Comment";
        String[] expecteds = new String[] { //
                "Block Comment", //
                "MappingNode", //
        };

        Composer sut = newComposerWithCommentsEnabled(data);
        List<Node> result = getNodeList(sut);

        printNodeList(result);
        assertNodesEqual(expecteds, result);
    }

    @Test
    public void testCommentEndingALine() {
        String data = "" + //
                "key: # Comment\n" + //
                "  value\n";

        String[] expecteds = new String[] { //
                "MappingNode", //
                "    Tuple", //
                "        ScalarNode: key", //
                "            InLine Comment", //
                "        ScalarNode: value" //
        };

        Composer sut = newComposerWithCommentsEnabled(data);
        List<Node> result = getNodeList(sut);

        printNodeList(result);
        assertNodesEqual(expecteds, result);
    }

    @Test
    public void testMultiLineComment() {
        String data = "" + //
                "key: # Comment\n" + //
                "     # lines\n" + //
                "  value\n" + //
                "\n";

        String[] expecteds = new String[] { //
                "MappingNode", //
                "    Tuple", //
                "        ScalarNode: key", //
                "            InLine Comment", //
                "            InLine Comment", //
                "        ScalarNode: value" //
        };

        Composer sut = newComposerWithCommentsEnabled(data);
        List<Node> result = getNodeList(sut);

        printNodeList(result);
        assertNodesEqual(expecteds, result);
    }

    @Test
    public void testBlankLine() {
        String data = "" + //
                "\n";

        String[] expecteds = new String[] { //
                "Block Comment", //
                "MappingNode", //
        };

        Composer sut = newComposerWithCommentsEnabled(data);
        List<Node> result = getNodeList(sut);

        printNodeList(result);
        assertNodesEqual(expecteds, result);
    }

    @Test
    public void testBlankLineComments() {
        String data = "" + //
                "\n" + //
                "abc: def # commment\n" + //
                "\n" + //
                "\n";

        String[] expecteds = new String[] { //
                "Block Comment", //
                "MappingNode", //
                "    Tuple", //
                "        ScalarNode: abc", //
                "        ScalarNode: def", //
                "            InLine Comment", //
                "End Comment", //
                "End Comment", //
        };

        Composer sut = newComposerWithCommentsEnabled(data);
        List<Node> result = getNodeList(sut);

        printNodeList(result);
        assertNodesEqual(expecteds, result);
    }

    @Test
    public void test_blockScalar() {
        String data = "" + //
                "abc: > # Comment\n" + //
                "    def\n" + //
                "    hij\n" + //
                "\n";

        String[] expecteds = new String[] { //
                "MappingNode", //
                "    Tuple", //
                "        ScalarNode: abc", //
                "            InLine Comment", //
                "        ScalarNode: def hij" //
        };

        Composer sut = newComposerWithCommentsEnabled(data);
        List<Node> result = getNodeList(sut);

        printNodeList(result);
        assertNodesEqual(expecteds, result);
    }

    @Test
    public void testDirectiveLineEndComment() {
        String data = "%YAML 1.1 #Comment\n";

        String[] expecteds = new String[] { //
                "" //
        };

        Composer sut = newComposerWithCommentsEnabled(data);
        List<Node> result = getNodeList(sut);

        printNodeList(result);
        assertNodesEqual(expecteds, result);
    }

    @Test
    public void testSequence() {
        String data = "" + //
                "# Comment\n" + //
                "list: # InlineComment1\n" + //
                "# Block Comment\n" + //
                "- item # InlineComment2\n" + //
                "# Comment\n";

        String[] expecteds = new String[] { //
                "Block Comment", //
                "MappingNode", //
                "    Tuple", //
                "        ScalarNode: list", //
                "            InLine Comment", //
                "        Block Comment", //
                "        SequenceNode", //
                "            ScalarNode: item", //
                "                InLine Comment", //
                "End Comment" //
        };

        Composer sut = newComposerWithCommentsEnabled(data);
        List<Node> result = getNodeList(sut);

        printNodeList(result);
        assertNodesEqual(expecteds, result);
    }

    @Test
    public void testAllComments1() throws Exception {
        String data = "" + //
                "# Block Comment1\n" + //
                "# Block Comment2\n" + //
                "key: # Inline Comment1a\n" + //
                "     # Inline Comment1b\n" + //
                "  # Block Comment3a\n" + //
                "  # Block Comment3b\n" + //
                "  value # Inline Comment2\n" + //
                "# Block Comment4\n" + //
                "list: # InlineComment3a\n" + //
                "      # InlineComment3b\n" + //
                "# Block Comment5\n" + //
                "- item1 # InlineComment4\n" + //
                "- item2: [ value2a, value2b ] # InlineComment5\n" + //
                "- item3: { key3a: [ value3a1, value3a2 ], key3b: value3b } # InlineComment6\n" + //
                "# Block Comment6\n" + //
                "---\n" + //
                "# Block Comment7\n" + //
                "";

        String[] expecteds = new String[] { //
                "Block Comment", //
                "Block Comment", //
                "MappingNode", //
                "    Tuple", //
                "        ScalarNode: key", //
                "            InLine Comment", //
                "            InLine Comment", //
                "        Block Comment", //
                "        Block Comment", //
                "        ScalarNode: value", //
                "            InLine Comment", //
                "    Tuple", //
                "        Block Comment", //
                "        ScalarNode: list", //
                "            InLine Comment", //
                "            InLine Comment", //
                "        Block Comment", //
                "        SequenceNode", //
                "            ScalarNode: item1", //
                "                InLine Comment", //
                "            MappingNode", //
                "                Tuple", //
                "                    ScalarNode: item2", //
                "                    SequenceNode", //
                "                        ScalarNode: value2a", //
                "                        ScalarNode: value2b", //
                "                        InLine Comment", //
                "            MappingNode", //
                "                Tuple", //
                "                    ScalarNode: item3", //
                "                    MappingNode", //
                "                        Tuple", //
                "                            ScalarNode: key3a", //
                "                            SequenceNode", //
                "                                ScalarNode: value3a1", //
                "                                ScalarNode: value3a2", //
                "                        Tuple", //
                "                            ScalarNode: key3b", //
                "                            ScalarNode: value3b", //
                "                        InLine Comment", //
                "End Comment", //
                "Block Comment", //
                "ScalarNode: ", // FIXME: should not be here
        };

        Composer sut = newComposerWithCommentsEnabled(data);
        List<Node> result = getNodeList(sut);

        printNodeList(result);
        assertNodesEqual(expecteds, result);
    }

    @Test
    public void testAllComments2() throws Exception {
        String data = "" + //
                "# Block Comment1\n" + //
                "# Block Comment2\n" + //
                "- item1 # Inline Comment1a\n" + //
                "        # Inline Comment1b\n" + //
                "# Block Comment3a\n" + //
                "# Block Comment3b\n" + //
                "- item2: value # Inline Comment2\n" + //
                "# Block Comment4\n" + //
                "";

        String[] expecteds = new String[] { //
                "Block Comment", //
                "Block Comment", //
                "SequenceNode", //
                "    ScalarNode: item1", //
                "        InLine Comment", //
                "        InLine Comment", //
                "    Block Comment", //
                "    Block Comment", //
                "    MappingNode", //
                "        Tuple", //
                "            ScalarNode: item2", //
                "            ScalarNode: value", //
                "                InLine Comment", //
                "End Comment", //
        };

        Composer sut = newComposerWithCommentsEnabled(data);
        List<Node> result = getNodeList(sut);
        Node newNode = new Yaml().compose(new StringReader("a: b"));
        result.add(newNode);

        printNodeList(result);
        assertNodesEqual(expecteds, result);
    }

    @Test
    public void testAllComments3() throws Exception {
        String data = "" + //
                "# Block Comment1\n" + //
                "[ item1, item2: value2, {item3: value3} ] # Inline Comment1\n" + //
                "# Block Comment2\n" + //
                "";

        String[] expecteds = new String[] { //
                "Block Comment", //
                "SequenceNode", //
                "    ScalarNode: item1", //
                "    MappingNode", //
                "        Tuple", //
                "            ScalarNode: item2", //
                "            ScalarNode: value2", //
                "    MappingNode", //
                "        Tuple", //
                "            ScalarNode: item3", //
                "            ScalarNode: value3", //
                "    InLine Comment", //
                "End Comment", //
        };

        Composer sut = newComposerWithCommentsEnabled(data);
        List<Node> result = getNodeList(sut);

        printNodeList(result);
        assertNodesEqual(expecteds, result);
    }

    @Test
    public void testGetSingleNode() {
        String data = "" + //
                "\n" + //
                "abc: def # commment\n" + //
                "\n" + //
                "\n";
        String[] expecteds = new String[] { //
                "MappingNode", //
                "    Tuple", //
                "        ScalarNode: abc", //
                "        ScalarNode: def", //
                "            InLine Comment", //
                "End Comment", //
                "End Comment", //
        };

        Composer sut = newComposerWithCommentsEnabled(data);
        List<Node> result = Arrays.asList(new Node[] { sut.getSingleNode() });

        printNodeList(result);
        assertNodesEqual(expecteds, result);
    }

    private static class TestConstructor extends SafeConstructor {
    }

    @Test
    public void testBaseConstructorGetData() {
        String data = "" + //
                "\n" + //
                "abc: def # commment\n" + //
                "\n" + //
                "\n";

        TestConstructor sut = new TestConstructor();
        sut.setComposer(newComposerWithCommentsEnabled(data));
        Object result = sut.getData();
        assertTrue(result instanceof LinkedHashMap);
        @SuppressWarnings("unchecked")
        LinkedHashMap<String, Object> map = (LinkedHashMap<String, Object>) result;
        assertEquals(1, map.size());
        assertEquals(map.get("abc"), "def");
    }
}
