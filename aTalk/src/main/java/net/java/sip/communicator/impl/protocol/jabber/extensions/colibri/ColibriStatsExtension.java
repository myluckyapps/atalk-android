/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.colibri;

import java.util.Collections;
import java.util.List;

import net.java.sip.communicator.impl.protocol.jabber.extensions.AbstractPacketExtension;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.XmlStringBuilder;

/**
 * Implements the Jitsi Videobridge <tt>stats</tt> extension within COnferencing with LIghtweight
 * BRIdging that will provide various statistics.
 *
 * @author Hristo Terezov
 * @author Eng Chong Meng
 */
public class ColibriStatsExtension extends AbstractPacketExtension
{
	/**
	 * The XML element name of the Jitsi Videobridge <tt>stats</tt> extension.
	 */
	public static final String ELEMENT_NAME = "stats";

	/**
	 * The XML COnferencing with LIghtweight BRIdging namespace of the Jitsi Videobridge
	 * <tt>stats</tt> extension.
	 */
	public static final String NAMESPACE = "http://jitsi.org/protocol/colibri";

	/**
	 * Constructs new <tt>ColibriStatsExtension</tt>
	 */
	public ColibriStatsExtension()
	{
		super(ELEMENT_NAME, NAMESPACE);
	}

	/**
	 * Adds stat extension.
	 * 
	 * @param stat
	 *        the stat to be added
	 */
	public void addStat(Stat stat)
	{
		addChildExtension(stat);
	}

	@Override
	public List<? extends ExtensionElement> getChildExtensions()
	{
		return Collections.unmodifiableList(super.getChildExtensions());
	}

	public static class Stat extends AbstractPacketExtension
	{
		/**
		 * The XML element name of a <tt>content</tt> of a Jitsi Videobridge <tt>stats</tt> IQ.
		 */
		public static final String ELEMENT_NAME = "stat";

		/**
		 * The XML name of the <tt>name</tt> attribute of a <tt>stat</tt> of a <tt>stats</tt> IQ
		 * which represents the <tt>name</tt> property of the statistic.
		 */
		public static final String NAME_ATTR_NAME = "name";

		/**
		 * The XML name of the <tt>name</tt> attribute of a <tt>stat</tt> of a <tt>stats</tt> IQ
		 * which represents the <tt>value</tt> property of the statistic.
		 */
		public static final String VALUE_ATTR_NAME = "value";

		public Stat()
		{
			super(ELEMENT_NAME, NAMESPACE);
		}

		/**
		 * Constructs new <tt>Stat</tt> by given name and value.
		 * 
		 * @param name
		 *        the name
		 * @param value
		 *        the value
		 */
		public Stat(String name, Object value)
		{
			this();
			this.setName(name);
			this.setValue(value);
		}

		@Override
		public String getElementName()
		{
			return ELEMENT_NAME;
		}

		/**
		 * @return the name
		 */
		public String getName()
		{
			return getAttributeAsString(NAME_ATTR_NAME);
		}

		@Override
		public String getNamespace()
		{
			return NAMESPACE;
		}

		/**
		 * @return the value
		 */
		public Object getValue()
		{
			return getAttribute(VALUE_ATTR_NAME);
		}

		/**
		 * @param name
		 *        the name to set
		 */
		public void setName(String name)
		{
			setAttribute(NAME_ATTR_NAME, name);
		}

		/**
		 * @param value
		 *        the value to set
		 */
		public void setValue(Object value)
		{
			setAttribute(VALUE_ATTR_NAME, value);
		}

		@Override
		public XmlStringBuilder toXML()
		{
			XmlStringBuilder xml = new XmlStringBuilder();

			String name = getName();
			Object value = getValue();

			if ((name != null) && (value != null)) {
				xml.halfOpenElement(ELEMENT_NAME);
				xml.optElement(NAME_ATTR_NAME, name);
				xml.optElement(VALUE_ATTR_NAME, value.toString());
				xml.append("/>");
			}
			return xml;
		}
	}
}