/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import net.java.sip.communicator.service.certificate.CertificateService;
import net.java.sip.communicator.service.protocol.AbstractProtocolProviderService;
import net.java.sip.communicator.service.protocol.AccountID;
import net.java.sip.communicator.service.protocol.ProtocolProviderFactory;
import net.java.sip.communicator.service.protocol.RegistrationState;
import net.java.sip.communicator.service.protocol.SecurityAuthority;
import net.java.sip.communicator.service.protocol.UserCredentials;
import net.java.sip.communicator.service.protocol.event.RegistrationStateChangeEvent;
import net.java.sip.communicator.util.Logger;

import org.atalk.android.aTalkApp;
import org.atalk.android.gui.login.IBRCaptchaProcessDialog;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jxmpp.jid.parts.Resourcepart;

import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

/**
 * Login to Jabber using username & password.
 *
 * @author Stefan Sieber
 * @author Eng Chong Meng
 */
public class LoginByPasswordStrategy implements JabberLoginStrategy
{
    /**
     * Logger of this class
     */
    private static final Logger logger = Logger.getLogger(LoginByPasswordStrategy.class);

    private final AbstractProtocolProviderService protocolProvider;
    private final AccountID accountID;
    private String password;

    /**
     * Create a login strategy that logs in using user credentials (username and password)
     *
     * @param protocolProvider protocol provider service to fire registration change events.
     * @param accountID The accountID to use for the login.
     */
    public LoginByPasswordStrategy(AbstractProtocolProviderService protocolProvider, AccountID accountID)
    {
        this.protocolProvider = protocolProvider;
        this.accountID = accountID;
    }

    /**
     * Loads the account passwords as preparation for the login.
     *
     * @param authority SecurityAuthority to obtain the password
     * @param reasonCode reasonCode why we're preparing for login
     * @param reason the reason descriptive text why we're preparing for login
     * @param isShowAlways <tt>true</tt> always show the credential prompt for user entry
     * @return UserCredentials in case they need to be cached for this session
     * (i.e. password is not persistent)
     */
    public UserCredentials prepareLogin(SecurityAuthority authority, int reasonCode,
            String reason, Boolean isShowAlways)
    {
        return loadPassword(authority, reasonCode, reason, isShowAlways);
    }

    /**
     * Determines whether the strategy is ready to perform the login.
     *
     * @return True when the password was successfully loaded.
     */
    public boolean loginPreparationSuccessful()
    {
        return password != null;
    }

    /**
     * Performs the login on an XMPP connection using SASL PLAIN.
     *
     * @param connection The connection on which the login is performed.
     * @param userName The full Jid username for the login.
     * @param resource The XMPP resource.
     * @return always true.
     * @throws XMPPException
     */
    public boolean login(XMPPTCPConnection connection, String userName, Resourcepart resource)
            throws XMPPException, SmackException
    {
        try {
            connection.login(userName, password, resource);
        } catch (IOException | InterruptedException ex) {
            // No response received within reply timeout. Timeout was 5000ms (~10s).
            // Rethrow XMPPException will trigger a re-login dialog
            String exMsg = ex.getMessage();
            XMPPError.Builder xmppErrorBuilder = XMPPError.from(XMPPError.Condition.not_authorized, exMsg);
            xmppErrorBuilder.setType(XMPPError.Type.CANCEL);
            throw new XMPPException.XMPPErrorException(null, xmppErrorBuilder.build());
        }
        return true;
    }

    /**
     * Perform the InBand Registration for the accountId on the defined XMPP connection by pps.
     * Registration can either be:
     * - simple username and password or
     * - With captcha protection using form with embedded captcha image if available, else the
     * image is retrieved from the given url in the form.
     *
     * @param pps The protocolServiceProvider.
     * @param accountId The username accountID for registration.
     */
    public boolean registerAccount(final ProtocolProviderServiceJabberImpl pps, final AccountID accountId)
    {
        // Wait for right moment before proceed, otherwise captcha dialog will be
        // obscured by other launching activities in progress.
        aTalkApp.waitForDisplay();
        new Handler(Looper.getMainLooper()).post(new Runnable()
        {
            @Override
            public void run()
            {
                Context context = aTalkApp.getCurrentActivity();
                IBRCaptchaProcessDialog mCaptchaDialog = new IBRCaptchaProcessDialog(context, pps, accountId, password);
                mCaptchaDialog.show();
            }
        });
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see net.java.sip.communicator.impl.protocol.jabber.JabberLoginStrategy# isTlsRequired()
     */
    public boolean isTlsRequired()
    {
        // requires TLS by default (i.e. it will not connect to a non-TLS server and will not
        // fallback to clear-text)
        return !accountID.getAccountPropertyBoolean(ProtocolProviderFactory.IS_ALLOW_NON_SECURE, false);
    }

    /**
     * Prepares an SSL Context that is customized SSL context.
     *
     * @param cs The certificate service that provides the context.
     * @param trustManager The TrustManager to use within the context.
     * @return An initialized context for the current provider.
     * @throws GeneralSecurityException
     */
    public SSLContext createSslContext(CertificateService cs, X509TrustManager trustManager)
            throws GeneralSecurityException
    {
        return cs.getSSLContext(trustManager);
    }

    /**
     * Load the password from the account configuration or ask the user.
     *
     * @param authority SecurityAuthority
     * @param reasonCode the authentication reason code. Indicates the reason of this authentication.
     * @return The UserCredentials in case they should be cached for this session (i.e. are not
     * persistent)
     */
    private UserCredentials loadPassword(SecurityAuthority authority, int reasonCode, String loginReason, boolean isShowAlways)
    {
        /*
         * Get the persistent password from the database if unavailable from accountID
         * Note: the last password entered by user is only available in accountID i.e mAccountProperties until
         * it is save in persistent database if enabled.
         */
        password = accountID.getPassword();
        if (TextUtils.isEmpty(password))
            password = JabberActivator.getProtocolProviderFactory().loadPassword(accountID);

        if ((password == null) || isShowAlways) {
            // create a default credentials object
            UserCredentials credentials = new UserCredentials();
            credentials.setUserName(accountID.getUserID());
            credentials.setLoginReason(loginReason);
            // Update password is found in DB else leave password field empty
            if (password != null) {
                credentials.setPassword(password.toCharArray());
            }

            // request account settings from the user and also show user default server option
            credentials = authority.obtainCredentials(accountID, credentials, reasonCode, true);

            // just return the bare credential in case user has canceled the login window
            if (credentials.isUserCancel()) {
                protocolProvider.fireRegistrationStateChanged(
                        protocolProvider.getRegistrationState(), RegistrationState.UNREGISTERED,
                        RegistrationStateChangeEvent.REASON_USER_REQUEST, "User cancel credentials request");
                return credentials;
            }

            // Extract the password the user entered. If no password specified, then canceled the operation
            char[] pass = credentials.getPassword();
            if (pass == null) {
                protocolProvider.fireRegistrationStateChanged(
                        protocolProvider.getRegistrationState(), RegistrationState.UNREGISTERED,
                        RegistrationStateChangeEvent.REASON_USER_REQUEST, "No password entered");
                return null;
            }

            // update password for this instance and save the password in accountID for later use by login()
            password = new String(pass);
            accountID.setPassword(password);

            boolean passwordPersistent = credentials.isPasswordPersistent();
            accountID.setPasswordPersistent(passwordPersistent);
            accountID.setIbRegistration(credentials.isIbRegistration());
            accountID.setServerOverridden(credentials.isServerOverridden());
            accountID.setServerAddress(credentials.getServerAddress());
            if (credentials.isServerOverridden()) {
                accountID.setServerPort(credentials.getServerPort());
                accountID.setDnssMode(credentials.getDnssecMode());
            }

            // must save to DB in case user makes changes
            JabberActivator.getProtocolProviderFactory().storeAccount(accountID);
            return credentials;
        }
        /*
         * Must always keep a copy of password in accountID.PASSWORD. Otherwise mAccountProperty.PASSWORD is null
         * when user changes the user registration state; This will clear the DB accountProperties ENCRYPTED_PASSWORD
         */
        else {
            accountID.setPassword(password);
        }
        return null;
    }
}
