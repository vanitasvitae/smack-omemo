package org.jivesoftware.smackx.mix.pam;

import static org.jivesoftware.smackx.mix.core.MixCoreConstants.URN_XMPP_MIX;

public class MixPamConstants {

    private static final String NAMESPACE_PAM_BARE = URN_XMPP_MIX + ":pam";

    public static final String NAMESPACE_PAM_2 = NAMESPACE_PAM_BARE + ":2";

    public static final String FEATURE_PAM_2 = NAMESPACE_PAM_2;

    public static final String FEATURE_PAM_ARCHIVE_2 = FEATURE_PAM_2 + "#archive";
}
