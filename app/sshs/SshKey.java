/**
 * Yobi, Project Hosting SW
 *
 * Copyright 2015 NAVER Corp.
 * http://yobi.io
 *
 * @author KiSeong Park
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

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;

import org.apache.commons.codec.binary.Base64;
import org.apache.sshd.common.SshException;
import org.apache.sshd.common.util.Buffer;
import org.eclipse.jgit.lib.Constants;

public class SshKey implements Serializable {
    private static final long serialVersionUID = 1L;
    private String rawData;
    private PublicKey publicKey;
    private String publicKeyBase64;

    private String comment;
    private String fingerprintResult;
    private String toStringResult;

    public SshKey(final String data, String comment) {
        this.rawData = data;
        this.comment = comment;
    }

    public SshKey(final PublicKey key) {
        this.publicKey = key;
        this.comment = "";
    }

    public PublicKey getPublicKey() {
        if (publicKey == null && rawData != null) {
            final String[] parts = rawData.split(" ", 3);
            if (comment == null && parts.length == 3) {
                comment = parts[2];
            }
            final byte[] bin = Base64.decodeBase64(Constants.encodeASCII(parts[1]));
            try {
                publicKey = new Buffer(bin).getRawPublicKey();
            } catch (SshException e) {
                throw new RuntimeException(e);
            }
        }
        return publicKey;
    }

    public String getAlgorithm() {
        return getPublicKey().getAlgorithm();
    }

    public String getComment() {
        if (isEmpty(comment) && rawData != null) {
            final String[] parts = rawData.split(" ", 3);
            if (parts.length == 3) {
                comment = parts[2];
            } else {
                comment = parts[1].substring(0,18);
            }
        }
        return comment;
    }

    public void setComment(final String comment) {
        this.comment = comment;
        if (rawData != null) {
            rawData = null;
        }
    }

    public String getRawData() {
        if (rawData == null && publicKey != null) {
            final String publicKeyB64 = getPublicKeyB64();
            final String comment = getComment();
            rawData = publicKeyB64 + (isEmpty(comment) ? "" : " " + comment);
        }
        return rawData;
    }

    public String getPublicKeyB64() {
        if (rawData == null && publicKey != null) {
            final Buffer buf = new Buffer();

            buf.putRawPublicKey(publicKey);
            final String alg = buf.getString();

            buf.clear();
            buf.putPublicKey(publicKey);
            final String b64 = Base64.encodeBase64String(buf.getBytes());

            final String comment = getComment();
            publicKeyBase64 = alg + " " + b64;
        } else if (rawData != null) {
            String[] parts = rawData.split(" ", 3);
            if (parts.length > 1) {
                publicKeyBase64 = parts[0] +" "+ parts[1];
            } else {
                return null;
            }
        }
        return publicKeyBase64;
    }

    public String getFingerprint() {
        if (fingerprintResult == null) {
            final StringBuilder sBuilder = new StringBuilder();
            String hash;
            if (rawData == null) {
                hash = getMD5(getPublicKey().getEncoded());
            } else {
                final String[] parts = rawData.split(" ", 3);
                final byte [] bin = Base64.decodeBase64(Constants.encodeASCII(parts[1]));
                hash = getMD5(bin);
            }
            for (int i = 0; i < hash.length(); i += 2) {
                sBuilder.append(hash.charAt(i)).append(hash.charAt(i + 1)).append(':');
            }
            sBuilder.setLength(sBuilder.length() - 1);
            fingerprintResult = sBuilder.toString();
        }
        return fingerprintResult;
    }

    @Override
    public boolean equals(final Object object) {
        if (object instanceof PublicKey) {
            return getPublicKey().equals(object);
        } else if (object instanceof SshKey) {
            return getPublicKey().equals(((SshKey) object).getPublicKey());
        }
        return false;
    }

    public boolean isEmpty(final String value) {
        return value == null || value.trim().length() == 0;
    }

    public String getMD5(final byte... bytes) {
        try {
            final MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.reset();
            md5.update(bytes);
            final byte[] digest = md5.digest();

            final StringBuilder sBuilder = new StringBuilder(digest.length * 2);
            for (int i=0; i<digest.length; i++) {
                if ((digest[i] & 0xff) < 0x10) {
                    sBuilder.append('0');
                }
                sBuilder.append(Long.toString(digest[i] & 0xff, 16));
            }

            return sBuilder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
