/*
 * aTalk, android VoIP and Instant Messaging client
 * Copyright 2014 Eng Chong Meng
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jivesoftware.smackx.bob.provider;

import org.atalk.util.StringUtils;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smackx.bob.packet.BoB;
import org.xmlpull.v1.XmlPullParser;

/**
 * The <tt>BoBProvider</tt> is an extension element provider that is meant to be used for
 * thumbnail & captcha requests and responses. Implementing XEP-0231: Bits of Binary
 *
 * @author Eng Chong Meng
 */
public class BoBProvider extends ExtensionElementProvider<BoB>
{
    /**
     * Parses the given <tt>XmlPullParser</tt> into a BoB packet and returns it.
     * Note: parse first XmlPullParser.OPEN_TAG is already consumed on first entry.
     * XEP-0231: Bits of Binary
     *
     * <data xmlns='urn:xmpp:tmp:bob'
     * cid='sha1+8f35fef110ffc5df08d579a50083ff9308fb6242@bob.xmpp.org'/>
     * max-age='86400'
     * type='image/png'>
     * iVBORw0KGgoAAAANSUhEUgAAAAoAAAAKCAYAAACNMs+9AAAABGdBTUEAALGP
     * ch9//q1uH4TLzw4d6+ErXMMcXuHWxId3KOETnnXXV6MJpcq2MLaI97CER3N0
     * vr4MkhoXe0rZigAAAABJRU5ErkJggg==
     * </data>
     *
     * @see ExtensionElementProvider#parse(XmlPullParser, int)
     */
    @Override
    public BoB parse(XmlPullParser parser, int initialDepth)
            throws Exception
    {
        long maxAge = 0;
        String data = "";

        String cid = parser.getAttributeValue("", BoB.ATTR_CID);
        String age = parser.getAttributeValue("", BoB.ATTR_MAX_AGE);
        if (!StringUtils.isNullOrEmpty(age))
            maxAge = Integer.parseInt(age);
        String mimeType = parser.getAttributeValue("", "type");

//        outerloop: while (true) {
//            int eventType = parser.next();
//            switch (eventType) {
//                case XmlPullParser.TEXT:
//                    data = parser.getText();
//                    break;
//                case XmlPullParser.END_TAG:
//                    if (parser.getDepth() == initialDepth) {
//                        break outerloop;
//                    }
//                    break;
//            }
//        }

        boolean done = false;
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.TEXT) {
                data = parser.getText();
            }
            else if (eventType == XmlPullParser.END_TAG) {
                if (BoB.ELEMENT.equals(parser.getName())) {
                    done = true;
                }
            }
        }
        return new BoB(cid, maxAge, mimeType, data);
    }
}
