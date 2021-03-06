/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package org.atalk.android.gui.chat;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;

import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.ContactResource;
import net.java.sip.communicator.service.protocol.FileTransfer;
import net.java.sip.communicator.service.protocol.Message;
import net.java.sip.communicator.service.protocol.OperationSetBasicInstantMessaging;
import net.java.sip.communicator.service.protocol.OperationSetChatStateNotifications;
import net.java.sip.communicator.service.protocol.OperationSetContactCapabilities;
import net.java.sip.communicator.service.protocol.OperationSetFileTransfer;
import net.java.sip.communicator.service.protocol.OperationSetMessageCorrection;
import net.java.sip.communicator.service.protocol.OperationSetPresence;
import net.java.sip.communicator.service.protocol.OperationSetSmsMessaging;
import net.java.sip.communicator.service.protocol.OperationSetThumbnailedFileFactory;
import net.java.sip.communicator.service.protocol.PresenceStatus;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.service.protocol.event.ContactPresenceStatusChangeEvent;
import net.java.sip.communicator.service.protocol.event.ContactPresenceStatusListener;
import net.java.sip.communicator.service.protocol.event.MessageListener;
import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.FileUtils;
import net.java.sip.communicator.util.Logger;

import org.atalk.android.R;
import org.atalk.android.aTalkApp;
import org.jivesoftware.smackx.chatstates.ChatState;
import org.jivesoftware.smackx.omemo.OmemoManager;
import org.jxmpp.jid.EntityBareJid;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

/**
 * The single chat implementation of the <tt>ChatTransport</tt> interface that provides abstraction
 * to protocol provider access and its supported features available to the metaContact.
 *
 * @author Yana Stamcheva
 * @author Eng Chong Meng
 */
public class MetaContactChatTransport implements ChatTransport, ContactPresenceStatusListener
{
    /**
     * The logger.
     */
    private static final Logger logger = Logger.getLogger(MetaContactChatTransport.class);

    /**
     * The parent <tt>ChatSession</tt>, where this transport is available.
     */
    private final MetaContactChatSession parentChatSession;

    /**
     * The associated protocol <tt>Contact</tt>.
     */
    private final Contact contact;

    /**
     * The associated protocol provider service for the <tt>Contact</tt>.
     */
    private final ProtocolProviderService mPPS;

    /**
     * The resource associated with this contact.
     */
    private ContactResource contactResource;

    /**
     * <tt>true</tt> when a contact sends a message with XEP-0085 chat state notifications;
     * override contact disco#info no XEP-0085 feature advertised.
     */
    private static boolean isChatStateSupported = false;

    /**
     * The protocol presence operation set associated with this transport.
     */
    private final OperationSetPresence presenceOpSet;

    /**
     * The thumbnail default width.
     */
    private static final int THUMBNAIL_WIDTH = 64;

    /**
     * The thumbnail default height.
     */
    private static final int THUMBNAIL_HEIGHT = 64;

    /**
     * Indicates if only the resource name should be displayed.
     */
    private boolean isDisplayResourceOnly = false;

    /**
     * Creates an instance of <tt>MetaContactChatTransport</tt> by specifying the parent
     * <tt>chatSession</tt> and the <tt>contact</tt> associated with the transport.
     *
     * @param chatSession the parent <tt>ChatSession</tt>
     * @param contact the <tt>Contact</tt> associated with this transport
     */
    public MetaContactChatTransport(MetaContactChatSession chatSession, Contact contact)
    {
        this(chatSession, contact, null, false);
    }

    /**
     * Creates an instance of <tt>MetaContactChatTransport</tt> by specifying the parent
     * <tt>chatSession</tt>, <tt>contact</tt>, and the <tt>contactResource</tt>
     * associated with the transport.
     *
     * @param chatSession the parent <tt>ChatSession</tt>
     * @param contact the <tt>Contact</tt> associated with this transport
     * @param contactResource the <tt>ContactResource</tt> associated with the contact
     * @param isDisplayResourceOnly indicates if only the resource name should be displayed
     */
    public MetaContactChatTransport(MetaContactChatSession chatSession, Contact contact,
            ContactResource contactResource, boolean isDisplayResourceOnly)
    {
        this.parentChatSession = chatSession;
        this.contact = contact;
        this.contactResource = contactResource;
        this.isDisplayResourceOnly = isDisplayResourceOnly;
        mPPS = contact.getProtocolProvider();

        presenceOpSet = mPPS.getOperationSet(OperationSetPresence.class);
        if (presenceOpSet != null)
            presenceOpSet.addContactPresenceStatusListener(this);
        isChatStateSupported = (mPPS.getOperationSet(OperationSetChatStateNotifications.class) != null);

        // checking this can be slow so make sure its out of our way
        new Thread(new Runnable()
        {
            public void run()
            {
                checkImCaps();
            }
        }).start();
    }

    /**
     * If sending im is supported check it for supporting html messages if a font is set.
     * As it can be slow make sure its not on our way
     */
    private void checkImCaps()
    {
        if ((ConfigurationUtils.getChatDefaultFontFamily() != null)
                && (ConfigurationUtils.getChatDefaultFontSize() > 0)) {
            OperationSetBasicInstantMessaging imOpSet = mPPS.getOperationSet(OperationSetBasicInstantMessaging.class);

            if (imOpSet != null)
                imOpSet.isContentTypeSupported(ChatMessage.ENCODE_HTML, contact);
        }
    }

    /**
     * Returns the contact associated with this transport.
     *
     * @return the contact associated with this transport
     */
    public Contact getContact()
    {
        return contact;
    }

    /**
     * Returns the contact address corresponding to this chat transport.
     *
     * @return The contact address corresponding to this chat transport.
     */
    public String getName()
    {
        return contact.getAddress();
    }

    /**
     * Returns the display name corresponding to this chat transport.
     *
     * @return The display name corresponding to this chat transport.
     */
    public String getDisplayName()
    {
        return contact.getDisplayName();
    }

    /**
     * Returns the contact resource of this chat transport that encapsulate
     * contact information of the contact who is logged.
     *
     * @return The display name of this chat transport resource.
     */
    public ContactResource getContactResource()
    {
        return contactResource;
    }

    /**
     * Returns the resource name of this chat transport. This is for example the name of the
     * user agent from which the contact is logged.
     *
     * @return The display name of this chat transport resource.
     */
    public String getResourceName()
    {
        if (contactResource != null)
            return contactResource.getResourceName();
        return null;
    }

    public boolean isDisplayResourceOnly()
    {
        return isDisplayResourceOnly;
    }

    /**
     * Returns the presence status of this transport.
     *
     * @return the presence status of this transport.
     */
    public PresenceStatus getStatus()
    {
        if (contactResource != null)
            return contactResource.getPresenceStatus();
        else
            return contact.getPresenceStatus();
    }

    /**
     * Returns the <tt>ProtocolProviderService</tt>, corresponding to this chat transport.
     *
     * @return the <tt>ProtocolProviderService</tt>, corresponding to this chat transport.
     */
    public ProtocolProviderService getProtocolProvider()
    {
        return mPPS;
    }

    /**
     * Returns <code>true</code> if this chat transport supports instant
     * messaging, otherwise returns <code>false</code>.
     *
     * @return <code>true</code> if this chat transport supports instant
     * messaging, otherwise returns <code>false</code>.
     */
    public boolean allowsInstantMessage()
    {
        // First try to ask the capabilities operation set if such is available.
        OperationSetContactCapabilities capOpSet = mPPS.getOperationSet(OperationSetContactCapabilities.class);
        if (capOpSet != null) {
            if (contact.getJid().asEntityBareJidIfPossible() == null) {
                isChatStateSupported = false;
                return false;
            }
            if (capOpSet.getOperationSet(contact, OperationSetBasicInstantMessaging.class) != null) {
                return true;
            }
        }
        else if (mPPS.getOperationSet(OperationSetBasicInstantMessaging.class) != null)
            return true;

        return false;
    }

    /**
     * Returns <code>true</code> if this chat transport supports message corrections and false
     * otherwise.
     *
     * @return <code>true</code> if this chat transport supports message corrections and false
     * otherwise.
     */
    public boolean allowsMessageCorrections()
    {
        OperationSetContactCapabilities capOpSet
                = getProtocolProvider().getOperationSet(OperationSetContactCapabilities.class);
        if (capOpSet != null) {
            return capOpSet.getOperationSet(contact, OperationSetMessageCorrection.class) != null;
        }
        else {
            return mPPS.getOperationSet(OperationSetMessageCorrection.class) != null;
        }
    }

    /**
     * Returns <code>true</code> if this chat transport supports sms
     * messaging, otherwise returns <code>false</code>.
     *
     * @return <code>true</code> if this chat transport supports sms
     * messaging, otherwise returns <code>false</code>.
     */
    public boolean allowsSmsMessage()
    {
        // First try to ask the capabilities operation set if such is available.
        OperationSetContactCapabilities capOpSet
                = getProtocolProvider().getOperationSet(OperationSetContactCapabilities.class);
        if (capOpSet != null) {
            if (capOpSet.getOperationSet(contact, OperationSetSmsMessaging.class) != null) {
                return true;
            }
        }
        else if (mPPS.getOperationSet(OperationSetSmsMessaging.class) != null)
            return true;
        return false;
    }

    /**
     * Returns <code>true</code> if this chat transport supports chat state notifications,
     * otherwise returns <code>false</code>.
     * User SHOULD explicitly discover whether the Contact supports the protocol or negotiate the
     * use of chat state notifications with the Contact (e.g., via XEP-0155 Stanza Session
     * Negotiation).
     *
     * @return <code>true</code> if this chat transport supports chat state
     * notifications, otherwise returns <code>false</code>.
     */
    public boolean allowsChatStateNotifications()
    {
//		Object tnOpSet = mPPS.getOperationSet(OperationSetChatStateNotifications.class);
//		return ((tnOpSet != null) && isChatStateSupported);
        return isChatStateSupported;

    }

    public static void setChatStateSupport(boolean isEnable)
    {
        isChatStateSupported = isEnable;
    }

    /**
     * Returns <code>true</code> if this chat transport supports file transfer, otherwise returns
     * <code>false</code>.
     *
     * @return <code>true</code> if this chat transport supports file transfer, otherwise returns
     * <code>false</code>.
     */
    public boolean allowsFileTransfer()
    {
        Object ftOpSet = mPPS.getOperationSet(OperationSetFileTransfer.class);
        return (ftOpSet != null);
    }

    /**
     * Sends the given instant message through this chat transport, by specifying the mime type
     * (html or plain text).
     *
     * @param message The message to send.
     * @param encryptionType The encryption type of the message to send: @see ChatMessage Encryption Type
     * @param mimeType The mime type of the message to send: 1=text/html or 0=text/plain.
     */
    public void sendInstantMessage(String message, int encryptionType, int mimeType)
    {
        // If this chat transport does not support instant messaging we do nothing here.
        if (!allowsInstantMessage()) {
            aTalkApp.showToastMessage(R.string.service_gui_SEND_MESSAGE_NOT_SUPPORTED, getName());
            return;
        }

        OperationSetBasicInstantMessaging imOpSet = mPPS.getOperationSet(OperationSetBasicInstantMessaging.class);
        int encType = encryptionType |
                (imOpSet.isContentTypeSupported(ChatMessage.ENCODE_HTML) ? mimeType : ChatMessage.ENCODE_PLAIN);
        Message msg = imOpSet.createMessage(message, encType, "");

        ContactResource toResource = (contactResource != null) ? contactResource : ContactResource.BASE_RESOURCE;
        if (ChatMessage.ENCRYPTION_OMEMO == encryptionType) {
            OmemoManager omemoManager = OmemoManager.getInstanceFor(mPPS.getConnection());
            imOpSet.sendInstantMessage(contact, toResource, msg, null, omemoManager);
        }
        else {
            imOpSet.sendInstantMessage(contact, toResource, msg);
        }
    }

    /**
     * Sends <tt>message</tt> as a message correction through this transport, specifying the mime
     * type (html or plain text) and the id of the message to replace.
     *
     * @param message The message to send.
     * @param encryptionType The encryptionType of the message to send: @see ChatMessage Encryption Type
     * @param mimeType The encode Mode of the message to send: 1=text/html or 0=text/plain.
     * @param correctedMessageUID The ID of the message being corrected by this message.
     */
    public void sendInstantMessage(String message, int encryptionType, int mimeType, String correctedMessageUID)
    {
        if (!allowsMessageCorrections()) {
            return;
        }

        OperationSetMessageCorrection mcOpSet = mPPS.getOperationSet(OperationSetMessageCorrection.class);
        int encType = encryptionType |
                (mcOpSet.isContentTypeSupported(ChatMessage.ENCODE_HTML) ? mimeType : ChatMessage.ENCODE_PLAIN);
        Message msg = mcOpSet.createMessage(message, encType, "");

        ContactResource toResource = (contactResource != null) ? contactResource : ContactResource.BASE_RESOURCE;
        if (ChatMessage.ENCRYPTION_OMEMO == encryptionType) {
            OmemoManager omemoManager = OmemoManager.getInstanceFor(mPPS.getConnection());
            mcOpSet.sendInstantMessage(contact, toResource, msg, correctedMessageUID, omemoManager);
        }
        else {
            mcOpSet.sendInstantMessage(contact, toResource, msg, correctedMessageUID);
        }
    }

    /**
     * Determines whether this chat transport supports the supplied content type
     *
     * @param mimeType the mime type we want to check
     * @return <tt>true</tt> if the chat transport supports it and <tt>false</tt> otherwise.
     */
    public boolean isContentTypeSupported(int mimeType)
    {
        OperationSetBasicInstantMessaging imOpSet = mPPS.getOperationSet(OperationSetBasicInstantMessaging.class);

        return (imOpSet != null) && imOpSet.isContentTypeSupported(mimeType);
    }

    /**
     * Sends the given sms message trough this chat transport.
     *
     * @param phoneNumber phone number of the destination
     * @param messageText The message to send.
     * @throws Exception if the send operation is interrupted
     */
    public void sendSmsMessage(String phoneNumber, String messageText)
            throws Exception
    {
        // If this chat transport does not support sms messaging we do nothing here.
        if (allowsSmsMessage()) {
            // SMSManager.sendSMS(mPPS, phoneNumber, messageText);}
        }
    }

    /**
     * Whether a dialog need to be opened so the user can enter the destination number.
     *
     * @return <tt>true</tt> if dialog needs to be open.
     */
    public boolean askForSMSNumber()
    {
        // If this chat transport does not support sms messaging we do nothing here.
        if (!allowsSmsMessage())
            return false;

        OperationSetSmsMessaging smsOpSet = mPPS.getOperationSet(OperationSetSmsMessaging.class);
        return smsOpSet.askForNumber(contact);
    }

    /**
     * Sends the given sms message trough this chat transport.
     *
     * @param message the message to send
     * @throws Exception if the send operation is interrupted
     */
    public void sendSmsMessage(String message)
            throws Exception
    {
        // If this chat transport does not support sms messaging we do nothing here.
        if (allowsSmsMessage()) {
            // SMSManager.sendSMS(contact, message);
        }
    }

    /**
     * Sends a chat state notification.
     *
     * @param chatState the chat state notification to send
     */
    public void sendChatStateNotification(ChatState chatState)
    {
        // If this chat transport does not allow chat state notification then just return
        if (allowsChatStateNotifications()) {

            // if protocol is not registered or contact is offline don't try to send chat state
            // notifications
            if (mPPS.isRegistered()
                    && (contact.getPresenceStatus().getStatus() >= PresenceStatus.ONLINE_THRESHOLD)) {

                OperationSetChatStateNotifications tnOperationSet
                        = mPPS.getOperationSet(OperationSetChatStateNotifications.class);
                try {
                    tnOperationSet.sendChatStateNotification(contact, chatState);
                } catch (Exception ex) {
                    logger.error("Failed to send chat state notifications.", ex);
                }
            }
        }
    }

    /**
     * Sends the given file through this chat transport file transfer operation set.
     *
     * @param file the file to send
     * @return the <tt>FileTransfer</tt> object charged to transfer the file
     * @throws Exception if anything goes wrong
     */
    public FileTransfer sendFile(File file)
            throws Exception
    {
        return sendFile(file, false);
    }

    /**
     * Sends the given file through this chat transport file transfer operation set.
     *
     * @param file the file to send
     * @return the <tt>FileTransfer</tt> object charged to transfer the file
     * @throws Exception if anything goes wrong
     */
    private FileTransfer sendFile(File file, boolean isMultimediaMessage)
            throws Exception
    {
        // If this chat transport does not support file transfer we do nothing and just return.
        if (!allowsFileTransfer())
            return null;

        if (FileUtils.isImage(file.getName())) {
            // Create a thumbNailed file if possible.
            OperationSetThumbnailedFileFactory tfOpSet = mPPS.getOperationSet(OperationSetThumbnailedFileFactory.class);

            if (tfOpSet != null) {
                byte[] thumbnail = getFileThumbnail(file);

                if (thumbnail != null && thumbnail.length > 0) {
                    file = tfOpSet.createFileWithThumbnail(file, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT,
                            "image/png", thumbnail);
                }
            }
        }
        if (isMultimediaMessage) {
            OperationSetSmsMessaging smsOpSet = mPPS.getOperationSet(OperationSetSmsMessaging.class);
            if (smsOpSet == null)
                return null;
            return smsOpSet.sendMultimediaFile(contact, file);
        }
        else {
            OperationSetFileTransfer ftOpSet = mPPS.getOperationSet(OperationSetFileTransfer.class);
            return ftOpSet.sendFile(contact, file);
        }
    }

    /**
     * Sends the given SMS multimedia message trough this chat transport, leaving the
     * transport to choose the destination.
     *
     * @param file the file to send
     * @throws Exception if the send doesn't succeed
     */
    public FileTransfer sendMultimediaFile(File file)
            throws Exception
    {
        return sendFile(file, true);
    }

    /**
     * Returns the maximum file length supported by the protocol in bytes.
     *
     * @return the file length that is supported.
     */
    public long getMaximumFileLength()
    {
        OperationSetFileTransfer ftOpSet = mPPS.getOperationSet(OperationSetFileTransfer.class);
        return ftOpSet.getMaximumFileLength();
    }

    public void inviteChatContact(EntityBareJid contactAddress, String reason)
    {
    }

    /**
     * Returns the parent session of this chat transport. A <tt>ChatSession</tt> could contain
     * more than one transports.
     *
     * @return the parent session of this chat transport
     */
    public ChatSession getParentChatSession()
    {
        return parentChatSession;
    }

    /**
     * Adds an SMS message listener to this chat transport.
     *
     * @param l The message listener to add.
     */
    public void addSmsMessageListener(MessageListener l)
    {
        // If this chat transport does not support sms messaging we do nothing here.
        if (!allowsSmsMessage())
            return;

        OperationSetSmsMessaging smsOpSet = mPPS.getOperationSet(OperationSetSmsMessaging.class);
        smsOpSet.addMessageListener(l);
    }

    /**
     * Adds an instant message listener to this chat transport.
     *
     * @param l The message listener to add.
     */
    public void addInstantMessageListener(MessageListener l)
    {
        // If this chat transport does not support instant messaging we do nothing here.
        if (!allowsInstantMessage())
            return;

        OperationSetBasicInstantMessaging imOpSet = mPPS.getOperationSet(OperationSetBasicInstantMessaging.class);
        imOpSet.addMessageListener(l);
    }

    /**
     * Removes the given sms message listener from this chat transport.
     *
     * @param l The message listener to remove.
     */
    public void removeSmsMessageListener(MessageListener l)
    {
        // If this chat transport does not support sms messaging we do nothing here.
        if (!allowsSmsMessage())
            return;

        OperationSetSmsMessaging smsOpSet = mPPS.getOperationSet(OperationSetSmsMessaging.class);
        smsOpSet.removeMessageListener(l);
    }

    /**
     * Removes the instant message listener from this chat transport.
     *
     * @param l The message listener to remove.
     */
    public void removeInstantMessageListener(MessageListener l)
    {
        // If this chat transport does not support instant messaging we do nothing here.
        if (!allowsInstantMessage())
            return;

        OperationSetBasicInstantMessaging imOpSet = mPPS.getOperationSet(OperationSetBasicInstantMessaging.class);
        imOpSet.removeMessageListener(l);
    }

    /**
     * Indicates that a contact has changed its status.
     *
     * @param evt The presence event containing information about the contact status change.
     */
    public void contactPresenceStatusChanged(ContactPresenceStatusChangeEvent evt)
    {
        // If the contactResource is set then the status will be updated from the
        // MetaContactChatSession.
        // cmeng: contactResource condition removed to fix contact goes offline<->online
        if (evt.getSourceContact().equals(contact)
                && !evt.getOldStatus().equals(evt.getNewStatus())
            //&& (contactResource == null)
                ) {
            this.updateContactStatus();
        }
    }

    /**
     * Updates the status of this contact with the new given status.
     */
    private void updateContactStatus()
    {
        // Update the status of the given contact in the "send via" selector box.
        parentChatSession.getChatSessionRenderer().updateChatTransportStatus(this);
    }

    /**
     * Removes all previously added listeners.
     */
    public void dispose()
    {
        if (presenceOpSet != null)
            presenceOpSet.removeContactPresenceStatusListener(this);
    }

    /**
     * Returns the descriptor of this chat transport.
     *
     * @return the descriptor of this chat transport
     */
    public Object getDescriptor()
    {
        return contact;
    }

    /**
     * Sets the icon for the given file.
     *
     * @param file the file to set an icon for
     * @return the byte array containing the thumbnail
     */
    public static byte[] getFileThumbnail(File file)
    {
        byte[] imageData = null;

        if (FileUtils.isImage(file.getName())) {
            String imagePath = file.toString();
            try {
                FileInputStream fis = new FileInputStream(imagePath);
                Bitmap imageBitmap = BitmapFactory.decodeStream(fis);

                // check to ensure BitmapFactory can handle the MIME type
                if (imageBitmap != null) {
                    int width = imageBitmap.getWidth();
                    int height = imageBitmap.getHeight();

                    if (width > THUMBNAIL_WIDTH)
                        width = THUMBNAIL_WIDTH;
                    if (height > THUMBNAIL_HEIGHT)
                        height = THUMBNAIL_HEIGHT;

                    imageBitmap = ThumbnailUtils.extractThumbnail(imageBitmap, width, height);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                    imageData = baos.toByteArray();
                }
            } catch (FileNotFoundException e) {
                if (logger.isDebugEnabled())
                    logger.debug("Could not locate image file.", e);
                e.printStackTrace();
            }
        }
        return imageData;
    }
}
