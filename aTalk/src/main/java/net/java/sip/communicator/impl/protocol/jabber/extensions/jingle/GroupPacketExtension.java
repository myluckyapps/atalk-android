/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.jingle;

import net.java.sip.communicator.impl.protocol.jabber.extensions.AbstractPacketExtension;
import net.java.sip.communicator.impl.protocol.jabber.extensions.DefaultPacketExtensionProvider;

import org.xmlpull.v1.XmlPullParser;

import java.util.List;

/**
 * Jingle group packet extension(XEP-0338).
 *
 * @author Pawel Domas
 */
public class GroupPacketExtension extends AbstractPacketExtension
{
    /**
     * The name of the "group" element.
     */
    public static final String ELEMENT_NAME = "group";

    /**
     * The namespace for the "group" element.
     */
    public static final String NAMESPACE = "urn:xmpp:jingle:apps:grouping:0";

    /**
     * The name of the payload <tt>id</tt> SDP argument.
     */
    public static final String SEMANTICS_ATTR_NAME = "semantics";

    /**
     * Name of the "bundle" semantics.
     */
    public static final String SEMANTICS_BUNDLE = "BUNDLE";

    /**
     * Creates a new {@link GroupPacketExtension} instance with the proper element name and
     * namespace.
     */
    public GroupPacketExtension()
    {
        super(ELEMENT_NAME, NAMESPACE);
    }

    /**
     * Creates new <tt>GroupPacketExtension</tt> for BUNDLE semantics initialized with given <tt>contents</tt> list.
     *
     * @param contents the list that contains the contents to be bundled.
     * @return new <tt>GroupPacketExtension</tt> for BUNDLE semantics initialized with given <tt>contents</tt> list.
     */
    public static GroupPacketExtension createBundleGroup(List<ContentPacketExtension> contents)
    {
        GroupPacketExtension group = new GroupPacketExtension();
        group.setSemantics(SEMANTICS_BUNDLE);
        group.addContents(contents);
        return group;
    }

    /**
     * Gets the semantics of this group.
     *
     * @return the semantics of this group.
     */
    public String getSemantics()
    {
        return getAttributeAsString(SEMANTICS_ATTR_NAME);
    }

    /**
     * Sets the semantics of this group.
     */
    public void setSemantics(String semantics)
    {
        this.setAttribute(SEMANTICS_ATTR_NAME, semantics);
    }

    /**
     * Gets the contents of this group.
     *
     * @return the contents of this group.
     */
    public List<ContentPacketExtension> getContents()
    {
        return getChildExtensionsOfType(ContentPacketExtension.class);
    }

    /**
     * Sets the contents of this group. For each content from given <tt>contents</tt>list only it's
     * name is being preserved.
     *
     * @param contents the contents of this group.
     */
    public void addContents(List<ContentPacketExtension> contents)
    {
        for (ContentPacketExtension content : contents) {
            ContentPacketExtension copy = new ContentPacketExtension();
            copy.setName(content.getName());
            addChildExtension(copy);
        }
    }

    /**
     * Parses group extension content.
     *
     * @param parser an XML parser positioned at the packet's starting element.
     * @return new <tt>GroupPacketExtension</tt> initialized with parsed contents list.
     * @throws java.lang.Exception if an error occurs parsing the XML.
     */
    public static GroupPacketExtension parseExtension(XmlPullParser parser)
            throws Exception
    {
        GroupPacketExtension group = new GroupPacketExtension();

        String semantics = parser.getAttributeValue("", SEMANTICS_ATTR_NAME);
        if (semantics != null)
            group.setSemantics(semantics);

        boolean done = false;
        int eventType;
        String elementName;
        DefaultPacketExtensionProvider<ContentPacketExtension> contentProvider
                = new DefaultPacketExtensionProvider<>(ContentPacketExtension.class);
        while (!done) {
            eventType = parser.next();
            elementName = parser.getName();

            if (elementName.equals(ContentPacketExtension.ELEMENT_NAME)) {
                ContentPacketExtension content = contentProvider.parse(parser, 0);
                group.addChildExtension(content);
            }

            if ((eventType == XmlPullParser.END_TAG)
                    && parser.getName().equals(ELEMENT_NAME)) {
                done = true;
            }
        }
        return group;
    }
}
