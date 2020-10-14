package org.yaml.snakeyaml.comment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.yaml.snakeyaml.events.Event;
import org.yaml.snakeyaml.events.Event.ID;
import org.yaml.snakeyaml.parser.Parser;
import org.yaml.snakeyaml.parser.ParserImpl;
import org.yaml.snakeyaml.reader.StreamReader;

public class ParserWithCommentEnabledTest {

    private void assertEventListEquals(List<ID> expectedEventIdList, Parser parser) {
        for (ID expectedEventId : expectedEventIdList) {
            parser.checkEvent(expectedEventId);
            Event event = parser.getEvent();
            System.out.println("Expected: " + expectedEventId);
            System.out.println("Got: " + event);
            System.out.println();
            if (event == null) {
                fail("Missing event: " + expectedEventId);
            }
            assertEquals(expectedEventId, event.getEventId());
        }
    }

    @SuppressWarnings("unused")
    private void printEventList(Parser parser) {
        for (Event event = parser.getEvent(); event != null; event = parser.getEvent()) {
            System.out.println("Got: " + event);
            System.out.println();
        }
    }

    @Test
    public void testEmpty() {
        List<ID> expectedEventIdList = Arrays.asList(new ID[] { ID.StreamStart, ID.StreamEnd });

        String data = "";

        Parser sut = new ParserImpl(new StreamReader(data), true);

        assertEventListEquals(expectedEventIdList, sut);
    }

    @Test
    public void testParseWithOnlyComment() {
        String data = "# Comment";

        List<ID> expectedEventIdList = Arrays.asList(new ID[] { //
                ID.StreamStart, //
                ID.Comment, //
                ID.StreamEnd, //
        });

        Parser sut = new ParserImpl(new StreamReader(data), true);

        assertEventListEquals(expectedEventIdList, sut);
    }

    @Test
    public void testCommentEndingALine() {
        String data = "" + //
                "key: # Comment\n" + //
                "  value\n";

        List<ID> expectedEventIdList = Arrays.asList(new ID[] { ID.StreamStart, //
                ID.DocumentStart, //
                ID.MappingStart, //
                ID.Scalar, ID.Comment, ID.Scalar, //
                ID.MappingEnd, //
                ID.DocumentEnd, //
                ID.StreamEnd });

        Parser sut = new ParserImpl(new StreamReader(data), true);

        assertEventListEquals(expectedEventIdList, sut);
    }

    @Test
    public void testMultiLineComment() {
        String data = "" + //
                "key: # Comment\n" + //
                "     # lines\n" + //
                "  value\n" + //
                "\n";

        List<ID> expectedEventIdList = Arrays.asList(new ID[] { ID.StreamStart, //
                ID.DocumentStart, //
                ID.MappingStart, //
                ID.Scalar, ID.Comment, ID.Comment, ID.Scalar, //
                ID.MappingEnd, //
                ID.DocumentEnd, //
                ID.StreamEnd });

        Parser sut = new ParserImpl(new StreamReader(data), true);

        assertEventListEquals(expectedEventIdList, sut);
    }

    @Test
    public void testBlankLine() {
        String data = "" + //
                "\n";

        List<ID> expectedEventIdList = Arrays.asList(new ID[] { ID.StreamStart, //
                ID.Comment, //
                ID.StreamEnd });

        Parser sut = new ParserImpl(new StreamReader(data), true);

        assertEventListEquals(expectedEventIdList, sut);
    }

    @Test
    public void testBlankLineComments() {
        String data = "" + //
                "\n" + //
                "abc: def # commment\n" + //
                "\n" + //
                "\n";

        List<ID> expectedEventIdList = Arrays.asList(new ID[] { ID.StreamStart, //
                ID.Comment, //
                ID.DocumentStart, //
                ID.MappingStart, //
                ID.Scalar, ID.Scalar, ID.Comment, //
                ID.Comment, //
                ID.Comment, //
                ID.MappingEnd, //
                ID.DocumentEnd, //
                ID.StreamEnd });

        Parser sut = new ParserImpl(new StreamReader(data), true);

        assertEventListEquals(expectedEventIdList, sut);
    }

    @Test
    public void test_blockScalar() {
        String data = "" + //
                "abc: > # Comment\n" + //
                "    def\n" + //
                "    hij\n" + //
                "\n";

        List<ID> expectedEventIdList = Arrays.asList(new ID[] { //
                ID.StreamStart, //
                ID.DocumentStart, //
                ID.MappingStart, //
                ID.Scalar, ID.Comment, //
                ID.Scalar, //
                ID.MappingEnd, //
                ID.DocumentEnd, //
                ID.StreamEnd //
        });

        Parser sut = new ParserImpl(new StreamReader(data), true);

        assertEventListEquals(expectedEventIdList, sut);
    }

    @Test
    public void testDirectiveLineEndComment() {
        String data = "%YAML 1.1 #Comment\n";

        List<ID> expectedEventIdList = Arrays.asList(new ID[] { //
                ID.StreamStart, //
                ID.StreamEnd //
        });

        Parser sut = new ParserImpl(new StreamReader(data), true);
        assertEventListEquals(expectedEventIdList, sut);
    }
    
    @Test
    public void testSequence() {
        String data = "" + //
                "# Comment\n" + //
                "list: # InlineComment1\n" + //
                "# Block Comment\n" + //
                "- item # InlineComment2\n" + //
                "# Comment\n";
  
        List<ID> expectedEventIdList = Arrays.asList(new ID[] { //
                ID.StreamStart, //
                ID.Comment, //
                ID.DocumentStart, //
                ID.MappingStart, //
                ID.Scalar, ID.Comment, 
                ID.Comment, //
                ID.SequenceStart, //
                ID.Scalar, ID.Comment, //
                ID.Comment, //
                ID.SequenceEnd, //
                ID.MappingEnd, //
                ID.DocumentEnd, //
                ID.StreamEnd //
        });

        Parser sut = new ParserImpl(new StreamReader(data), true);
        
        assertEventListEquals(expectedEventIdList, sut);
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

        List<ID> expectedEventIdList = Arrays.asList(new ID[] { //
                ID.StreamStart, //
                ID.Comment, //
                ID.Comment, //
                ID.DocumentStart, //
                ID.MappingStart, //
                ID.Scalar, ID.Comment, ID.Comment, //

                ID.Comment, ID.Comment, //
                ID.Scalar, ID.Comment, //

                ID.Comment, //
                ID.Scalar, ID.Comment, ID.Comment, //
                ID.Comment, //

                ID.SequenceStart, //
                ID.Scalar, ID.Comment, //
                ID.MappingStart, //
                ID.Scalar, ID.SequenceStart, ID.Scalar, ID.Scalar, ID.SequenceEnd, ID.Comment, //
                ID.MappingEnd, 
                
                ID.MappingStart, //
                ID.Scalar, // value=item3
                ID.MappingStart, //
                ID.Scalar, // value=key3a
                ID.SequenceStart, //
                ID.Scalar, // value=value3a
                ID.Scalar, //value=value3a2
                ID.SequenceEnd, //
                ID.Scalar, // value=key3b
                ID.Scalar, // value=value3b
                ID.MappingEnd, //
                ID.Comment, // type=IN_LINE, value= InlineComment6
                ID.Comment, //
                ID.MappingEnd, //
                ID.SequenceEnd, //
                ID.MappingEnd, 
                ID.DocumentEnd, //

                ID.DocumentStart, //
                ID.Comment, //
                ID.Scalar, // Empty
                ID.DocumentEnd, //
                ID.StreamEnd //
        });

        Parser sut = new ParserImpl(new StreamReader(data), true);

        //printEventList(sut);
        assertEventListEquals(expectedEventIdList, sut);
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

        List<ID> expectedEventIdList = Arrays.asList(new ID[] { //
                ID.StreamStart, //
                ID.Comment, //
                ID.Comment, //
                ID.DocumentStart, //
                ID.SequenceStart, //
                ID.Scalar, ID.Comment, ID.Comment, //
                ID.Comment, //
                ID.Comment, //
                ID.MappingStart, //
                ID.Scalar, ID.Scalar, ID.Comment, //
                ID.Comment, //
                ID.MappingEnd, //
                ID.SequenceEnd, //
                ID.DocumentEnd, //
                ID.StreamEnd //
        });

        Parser sut = new ParserImpl(new StreamReader(data), true);

        assertEventListEquals(expectedEventIdList, sut);
    }

    @Test
    public void testAllComments3() throws Exception {
        String data = "" + //
                "# Block Comment1\n" + //
                "[ item1, item2: value2, {item3: value3} ] # Inline Comment1\n" + //
                "# Block Comment2\n" + //
                "";

        List<ID> expectedEventIdList = Arrays.asList(new ID[] { //
                ID.StreamStart, //
                ID.Comment, //
                ID.DocumentStart, //
                ID.SequenceStart, //
                ID.Scalar,
                ID.MappingStart, //
                ID.Scalar, ID.Scalar, //
                ID.MappingEnd, //
                ID.MappingStart, //
                ID.Scalar, ID.Scalar, //
                ID.MappingEnd, //
                ID.SequenceEnd, //
                ID.Comment, //
                ID.Comment, //
                ID.DocumentEnd, //
                ID.StreamEnd //
        });

        Parser sut = new ParserImpl(new StreamReader(data), true);

//        printEventList(sut);
        assertEventListEquals(expectedEventIdList, sut);
    }
}
