/**
 * Yobi, Project Hosting SW
 *
 * Copyright 2015 NAVER Corp.
 * http://yobi.io
 *
 * @author Hyeok Oh
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

import java.net.SocketAddress;

import models.User;

import org.apache.sshd.common.Session.AttributeKey;

public class SshDaemonClient {
    public static final AttributeKey<SshDaemonClient> KEY = new AttributeKey<SshDaemonClient>();

    private final SocketAddress remoteAddress;

    private User user;
    private SshKey key;

    SshDaemonClient(final SocketAddress peer) {
        this.remoteAddress = peer;
    }

    public SocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    public User getUser() {
        return user;
    }

    public void setUser(final User user) {
        this.user = user;
    }

    public SshKey getKey() {
        return key;
    }

    public void setKey(final SshKey key) {
        this.key = key;
    }
}
