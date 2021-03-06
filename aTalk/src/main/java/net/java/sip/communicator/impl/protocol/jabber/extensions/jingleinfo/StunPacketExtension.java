/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.jingleinfo;

import net.java.sip.communicator.impl.protocol.jabber.extensions.AbstractPacketExtension;

/**
 * Stun packet extension.
 *
 * @author Sebastien Vincent
 * @author Eng Chong Meng
 */
public class StunPacketExtension extends AbstractPacketExtension
{
	/**
	 * The namespace.
	 */
    public static final String NAMESPACE = "google:jingleinfo";

	/**
	 * The element name.
	 */
	public static final String ELEMENT_NAME = "stun";

	/**
	 * Constructor.
	 */
	public StunPacketExtension()
	{
		super(ELEMENT_NAME, NAMESPACE);
	}
}
