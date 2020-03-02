/**
 *
 * Copyright 2020 Paul Schaub
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jivesoftware.smackx.mix.pam;

import static org.jivesoftware.smackx.mix.core.MixCoreConstants.URN_XMPP_MIX;

public class MixPamConstants {

    private static final String NAMESPACE_PAM_BARE = URN_XMPP_MIX + ":pam";

    public static final String NAMESPACE_PAM_2 = NAMESPACE_PAM_BARE + ":2";

    public static final String FEATURE_PAM_2 = NAMESPACE_PAM_2;

    public static final String FEATURE_PAM_ARCHIVE_2 = FEATURE_PAM_2 + "#archive";
}
