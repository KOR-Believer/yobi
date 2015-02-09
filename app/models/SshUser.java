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
package models;

import java.util.Date;
import java.util.Locale;

import java.text.SimpleDateFormat;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.persistence.*;

import org.apache.commons.codec.binary.Base64;
import org.eclipse.jgit.lib.Constants;

import models.User;

import play.data.format.Formats;
import play.db.ebean.*;

@Table(name="SSH_STORE")
@Entity
public class SshUser extends Model {
    private static final long serialVersionUID = -1971453347598433331L;

    @Id
    @Column(columnDefinition="varchar(588)")
    public String sshKey;

    @ManyToOne
    public User user;
    public String comment;

    public String fingerPrint;

    @Formats.DateTime(pattern = "yyyy-MM-dd")
    public Date lastConnectedDate;

    @Formats.DateTime(pattern = "yyyy-MM-dd")
    public Date registerDate;

    public static Finder<String, SshUser> find = new Finder<String, SshUser>(String.class, SshUser.class);

    public boolean isKeyUsed(){
        if(lastConnectedDate==null) return false;
        return true;
    }

    public String getLastConnected() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.US);

        return sdf.format(this.lastConnectedDate);
    }

    public void updateLastConnectedDate() {
        this.lastConnectedDate = new Date();
        this.save();
    }

    public String getRegister() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
        return sdf.format(this.registerDate);
    }

    public static String fingerPrint(String key){
        if(key!=null){
            String hash;
            StringBuilder sb = new StringBuilder();
            byte [] bin = Base64.decodeBase64(Constants.encodeASCII(key));
            hash = getMD5(bin);

            for (int i = 0; i < hash.length(); i += 2) {
                sb.append(hash.charAt(i)).append(hash.charAt(i + 1)).append(':');
            }
            sb.setLength(sb.length() - 1);
            return sb.toString();
        }
        return "Unable to compute fingerprint";
    }

    public static String getMD5(byte... bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.reset();
            md.update(bytes);
            byte[] digest = md.digest();
            return toHex(digest);
        } catch (NoSuchAlgorithmException t) {
            throw new RuntimeException(t);
        }
    }
    public static String toHex(byte... bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (int i = 0; i < bytes.length; i++) {
            if ((bytes[i] & 0xff) < 0x10) {
                sb.append('0');
            }
            sb.append(Long.toString(bytes[i] & 0xff, 16));
        }
        return sb.toString();
    }

    public static String add(SshUser key) {
        key.registerDate = new Date();
        key.save();
        return key.sshKey;
    }

    public static SshUser findByKey(String keyString) {
        SshUser findKey = find.where().eq("sshKey", keyString).findUnique();
        return findKey;
    }
}
