/**
 * Yobi, Project Hosting SW
 *
 * Copyright 2015 NAVER Corp.
 * http://yobi.io
 *
 * @author Hyeok Oh, KiSeong Park
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package sshs;

import java.security.PublicKey;

import models.SshUser;

import org.apache.sshd.server.PublickeyAuthenticator;
import org.apache.sshd.server.session.ServerSession;

import utils.Constants;

public class SshPublicKeyAuth implements PublickeyAuthenticator {
    @Override
    public boolean authenticate(final String username, final PublicKey key, final ServerSession session) {
        // if username isn't 'yobi' -> deny (username@domain)
        if (!username.equals(Constants.SSH_USERNAME)) {
            return false;
        }

        return doAuthenticate(new SshKey(key), session);
    }

    private boolean doAuthenticate(final SshKey suppliedKey, final ServerSession session) {
        final String clientkey = suppliedKey.getPublicKeyB64();
        final SshUser result = SshUser.findByKey(clientkey);

        if (result != null) {
            // if public key exists
            result.updateLastConnectedDate();
            final SshDaemonClient client = session.getAttribute(SshDaemonClient.KEY);
            client.setUser(result.user);
            client.setKey(suppliedKey);
            return true;
        }
        // if public key not exists
        return false;
    }
}
