/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.jitsimeet;

import net.java.sip.communicator.impl.protocol.jabber.extensions.AbstractPacketExtension;
import net.java.sip.communicator.impl.protocol.jabber.extensions.DefaultPacketExtensionProvider;

import org.jivesoftware.smack.provider.ProviderManager;

/**
 * Packet extension used by Jitsi Meet to advertise local media SSRCs in MUC presence.
 *
 * @author Pawel Domas
 * @author Eng Chong Meng
 */
public class MediaPresenceExtension extends AbstractPacketExtension
{
    /**
     * The namespace of this packet extension.
     */
    public static final String NAMESPACE = "http://estos.de/ns/mjs";

    /**
     * XML element name of this packet extension.
     */
    public static final String ELEMENT_NAME = "media";

    /**
     * Creates an <tt>MediaPresenceExtension</tt> instance.
     */
    public MediaPresenceExtension()
    {
        super(ELEMENT_NAME, NAMESPACE);
    }

    /**
     * Registers packet Jitsi Meet media presence packet extensions in given <tt>providerManager</tt>.
     *
     * @param providerManager the <tt>ProviderManager</tt> to which media presence extensions will be registered to.
     */
    public static void registerExtensions(ProviderManager providerManager)
    {
        // <media>
        ProviderManager.addExtensionProvider(MediaPresenceExtension.ELEMENT_NAME, MediaPresenceExtension.NAMESPACE,
                new DefaultPacketExtensionProvider<>(MediaPresenceExtension.class));

        // <source>
        ProviderManager.addExtensionProvider(MediaPresenceExtension.Source.ELEMENT_NAME, MediaPresenceExtension.Source.NAMESPACE,
                new DefaultPacketExtensionProvider<>(Source.class));
    }

    /**
     * Add a <tt>Source</tt> object to the <tt>sourceList</tt> representing a
     * <source /> line.
     *
     * @arg type the type of the <tt>Source</tt>
     * @arg ssrc the ssrc of the <tt>Source</tt>
     * @arg direction the direction of the <tt>Source</tt>
     */
    public void addSource(String type, String ssrc, String direction)
    {
        Source s = new Source();
        s.setMediaType(type);
        s.setSSRC(ssrc);
        s.setDirection(direction);
        addChildExtension(s);
    }

    /**
     * Source extension element that specifies into about media SSRC.
     */
    public static class Source extends AbstractPacketExtension
    {
        /**
         * Source XML element name.
         */
        public final static String ELEMENT_NAME = "source";

        /**
         * No namespace attached.
         */
        public final static String NAMESPACE = "urn:xmpp:jingle:apps:rtp:ssma:0";

        /**
         * SSRC media type attribute name. Can be 'audio' or 'video'.
         */
        public final static String MEDIA_TYPE_ATTR_NAME = "type";

        /**
         * SSRC number attribute name.
         */
        public final static String SOURCE_ATTR_NAME = "ssrc";

        /**
         * Media direction as used in SDP('sendrecv', 'recvonly', 'sendonly').
         */
        public final static String DIRECTION_ATTR_NAME = "direction";

        /**
         * Creates new instance of <tt>Source</tt> packet extension with default 'sendrecv' direction set.
         */
        public Source()
        {
            super(ELEMENT_NAME, NAMESPACE);

            // Default direction
            setDirection("sendrecv");
        }

        /**
         * Returns media direction (values like used in SDP 'sendrecv', 'recvonly'...).
         */
        public String getDirection()
        {
            return getAttributeAsString(DIRECTION_ATTR_NAME);
        }

        /**
         * Returns media synchronization source identifier.
         */
        public String getSSRC()
        {
            return getAttributeAsString(SOURCE_ATTR_NAME);
        }

        /**
         * Sets media type of the media represented by this source packet extensions.
         *
         * @param mediaType media type string('audio' or 'video').
         */
        public void setMediaType(String mediaType)
        {
            setAttribute(MEDIA_TYPE_ATTR_NAME, mediaType);
        }

        /**
         * Direction of the media represented by this source packet extension.
         *
         * @param direction media direction like used in SDP ('sendrecv, 'recvonly'...).
         */
        public void setDirection(String direction)
        {
            setAttribute(DIRECTION_ATTR_NAME, direction);
        }

        /**
         * Sets synchronization source identifier of the media represented by this source packet extension.
         *
         * @param ssrc synchronization source identifier to set.
         */
        public void setSSRC(String ssrc)
        {
            setAttribute(SOURCE_ATTR_NAME, ssrc);
        }

        /**
         * @return type of the media(audio or video) represented by this source packet extension.
         */
        public String getMediaType()
        {
            return getAttributeAsString(MEDIA_TYPE_ATTR_NAME);
        }
    }
}
